# CryptoCraft Architecture

## Overview

CryptoCraft is a microservice-oriented prototype for inventory administration and dynamic menu pricing. The implemented path covers inventory CRUD, scheduled price generation, Kafka-based price events, and WebSocket price broadcasts. The repository's README describes a broader restaurant platform, but order management, reservations, payment processing, bonuses, and analytics are not implemented as services in the current codebase.

## System context

```mermaid
flowchart LR
    Admin["Admin browser<br/>React / Vite"] -->|HTTP /api| Gateway["API Gateway<br/>Spring Cloud Gateway"]
    Gateway -->|Eureka discovery| Inventory["Inventory Service<br/>Spring MVC / JPA"]
    Pricing["Pricing Service<br/>scheduler + pricing engine"] -->|HTTP GET /internal/prices| Inventory
    Inventory -->|JDBC + Liquibase| DB[("PostgreSQL")]
    Pricing -->|price-updates| Kafka[("Kafka")]
    Kafka --> Streaming["Price Streaming Service<br/>Kafka consumer"]
    Browser["TV / price clients"] <-->|WebSocket /ws/prices/{venue}| Streaming
    Gateway -.-> Eureka[("Eureka Server")]
    Inventory -.-> Eureka
    Pricing -.-> Eureka
    Streaming -.-> Eureka
```

Eureka is used for registering services and for all gateway routes; the Pricing service's Inventory client also resolves `inventory-service` through Eureka.

## Modules and responsibilities

| Module | Responsibility and key implementation |
|---|---|
| `admin-ui` | React 19 and TypeScript admin application. Provides brand, product, and item screens and a venue switcher (with "New venue" and a link to the venue's TV board); uses Axios for REST requests and TanStack Query for loading, mutations, and cache invalidation. |
| `api-gateway` | Spring Cloud Gateway (WebFlux), listening on port `8765`. Routes `/api/v1/brands/**`, `/api/v1/products/**`, `/api/v1/items/**` and `/api/v1/platform/tenants/**` to Inventory and `/ws/prices/**`, `/tv.html` to Price Streaming, all through Eureka; no other paths are public. |
| `eureka-server` | Spring Cloud Netflix Eureka registry, listening on port `8761`. |
| `inventory-service` | Spring MVC REST API on port `8081`; manages brands, products, items, recipes/item-product links, and product stock. Uses JPA/Hibernate, PostgreSQL, and Liquibase. Implements tenant context and PostgreSQL row-level security. |
| `pricing-service` | Spring service on port `8082`. Periodically loads item base/min/max prices from Inventory, calculates new prices, caches the current prices in memory, and publishes `PriceEvent`s to Kafka. |
| `price-streaming-service` | Spring WebFlux service on port `8083`. Consumes Kafka price events and sends each one to the screens of its venue. WebSocket endpoint: `/ws/prices/{venueSlug}`; TV page: `/tv.html?venue={venueSlug}`. |
| `authentication-service` | Separate Spring Security/OAuth2 module with Google/Apple client configuration and a minimal root controller. It is not included in the root Gradle `settings.gradle` or either Compose stack, and is not wired into the gateway in the current configuration. |
| `order-service` | Spring MVC service on port `8084` with its own database `orders`. Tables with QR tokens, visits (table sessions), anonymous guests (`X-Participant-Id`), orders with prices locked from Pricing, per-guest bills. Writes off recipes in Inventory after commit (retried if Inventory is down) and publishes `OrderPlaced` to Kafka topic `order-events`. |
| `tenant-support` | Shared library (auto-configuration) for tenant-scoped services: `X-Tenant-Id` filter, `TenantContext`, RLS-aware `TenantAwareDataSource`, `TenantEntityListener`. A service only provides a `TenantStatusLookup`. |
| `platform` | Gradle Java platform/BOM centralizing Spring Boot, Spring Cloud, PostgreSQL, MapStruct, and related dependency versions. |

## Request and event flows

### Admin inventory requests

1. The React admin UI calls `/api/v1/brands`, `/api/v1/products`, or `/api/v1/items`.
2. In the Vite development server, `/api` is proxied to the API Gateway (default `http://localhost:8765`). The UI's Axios interceptor can attach `X-Tenant-Id`.
3. The Gateway forwards the request to the Inventory Service through its configured Eureka route.
4. Inventory's servlet filter establishes tenant context, then its controllers, services, repositories, and JPA entities perform the operation.
5. Inventory reads or writes PostgreSQL. Liquibase applies schema changes at startup.

Product creation, for example, validates the request and referenced brand, checks for an existing product with the same case-insensitive name and brand, maps the DTO to a `Product` entity, persists it, and returns a `ProductResponseDto`.

### Dynamic price streaming

1. `PriceConfigScheduler` loads item price configuration of all active venues from Inventory's `GET /internal/prices` (venue slug and `price_tick_seconds`, item name/type, base/min/max price). It re-reads it every 60 seconds and retries every 5 seconds while it has none; a failed call keeps the previous configuration.
2. `PricingScheduler` runs every 100 ms and moves each item's price once per its venue's `price_tick_seconds`. A newly seen item is priced at once and then shifted by a random offset, so a venue's board changes item by item.
3. `PricingEngine` calculates a bounded price between the item's minimum and maximum and publishes a `PriceEvent` (venue id and slug, item id, name, type, price, version) to Kafka topic `price-updates`.
4. `PriceConsumer` in the Price Streaming service consumes that topic.
5. `PriceBroadcastService` keeps the latest price per item for each venue. A screen connecting to `/ws/prices/{venueSlug}` first receives that snapshot, then only that venue's changes.

Item changes do not wait for the next refresh: after an item is created, updated, put on or taken off the stop-list, or deleted, Inventory publishes its new price configuration to Kafka topic `item-config-updates` (after commit; a failed send is only logged). Pricing applies it to its cache and re-announces the item's current price unchanged, so screens show the new state within a second. Prices of items on the stop-list are frozen: announced once and not moved until the item is back on sale; the TV shows them dimmed with "SOLD OUT" and without change arrows or new candles.

There is no implemented flow connecting orders or payments to price changes. Pricing changes are currently generated by a random movement/correction algorithm.

### Placing an order

1. A guest scans a table's QR code: `POST /api/v1/table-sessions/join` joins the table's open visit (or opens one) and returns a `participantId`, the guest's token until Identity Service exists.
2. The cart lives on the client. Sending it (`POST /api/v1/orders`, `X-Participant-Id`, idempotent by `clientOrderId`) makes Order Service check the menu and stop-list in Inventory (`/api/v1/items/sellable`), lock the current prices from Pricing (`/internal/current-prices`) and store the order on the guest's bill.
3. After commit Order Service writes the recipes off in Inventory (`POST /api/v1/stock-consumptions`, idempotent by order id). If Inventory is down the order stays `PENDING` and `StockWriteOffRetrier` retries it.
4. `OrderPlaced` goes to Kafka topic `order-events` (best effort) for the kitchen/bar screens, demand-based pricing and analytics.

Bills are per guest (each pays for themselves); staff see the whole table split per guest and close the visit when it is paid.

## Data and persistence

Inventory stores the primary relational data in PostgreSQL:

- `brands`: product brands.
- `products`: name, description, image, stock quantity, unit, brand, and tenant.
- `items`: menu items, price configuration (`base_price`, `min_price`, `max_price`), stop-list flag (`stopped`) and menu position (`display_order`).
- `item_products`: recipe links from items to products, with quantities in the product's unit (`NUMERIC(12,3)`, as is `products.quantity_in_stock`).
- `stock_consumptions` / `stock_consumption_lines`: recipe write-offs per order (`POST /api/v1/stock-consumptions`, idempotent by venue + `orderId`). Stock never goes below zero; a missing amount is recorded as `requested` > `consumed` and returned as a shortage instead of rejecting the order.
- `tenants`: tenant records used by tenant-aware entities and policies.

Liquibase change sets create and evolve the schema, seed sample data and prices, and configure PostgreSQL row-level security for tenant-scoped inventory tables. Pricing's active configuration and current prices are in-memory caches; Kafka carries price events. No persistent order, payment, or price-history store is implemented.

## Technologies and runtime

- **Backend:** Java 21, Gradle, Spring Boot, Spring MVC, Spring WebFlux, Spring Data JPA, Spring Security/OAuth2 configuration, Spring Cloud Gateway, Eureka.
- **Database and messaging:** PostgreSQL, Liquibase, Kafka, Zookeeper (for the configured Kafka broker).
- **Frontend:** React, TypeScript, Vite, Material UI, TanStack Query, Axios.
- **Local orchestration:** Docker Compose, Kafka UI, PostgreSQL volume.

Each service has `application.yml` (shared), `application-local.yml` (services on the host, infrastructure in Docker; the default profile) and `application-prod.yml` (inside the compose network, addresses overridable by environment variables). `docker-compose.yml` is the development stack: every service in a container with the `prod` profile, debug ports, Kafka UI, healthchecks for start order, and Kafka with two listeners (`kafka:29092` for containers, `localhost:9092` for the host). `docker-compose-prod.yml` is the production-like stack: no debug ports, database and Kafka not published, secrets from `.env`, and the admin UI as a static nginx build that proxies `/api`, `/ws` and `/tv.html` to the gateway.

The gateway exposes only its explicit routes (inventory admin API, platform tenants, `/ws/prices/**`, `/tv.html`); the discovery locator is off so internal endpoints such as `/internal/prices` and `/api/v1/stock-consumptions` are not reachable through it. Eureka evicts dead instances within seconds (self-preservation off, 5 s lease renewal, 15 s expiry) and clients refresh the registry every 5 s.

## Security and tenant model

A tenant is a venue. All venues share one schema; every catalog table carries `tenant_id` and is protected by PostgreSQL row-level security (`FORCE ROW LEVEL SECURITY`, one policy per table on `app_current_tenant()`).

- The service connects as `cryptocraft_app` (no superuser, no `BYPASSRLS`); Liquibase migrates as `postgres`. Credentials: `APP_DB_USER` / `APP_DB_PASSWORD`.
- `TenantFilter` takes the tenant from the `X-Tenant-Id` header: missing or malformed → 400, unknown → 404, suspended → 403. `/internal/**`, `/api/v1/platform/**`, actuator and OpenAPI paths are not tenant-scoped.
- `TenantAwareDataSource` overwrites `app.tenant_id` on every connection checkout; without a tenant the value is empty, so queries see no rows and writes are rejected.
- Cross-tenant work goes through `SECURITY DEFINER` functions: `internal_item_prices()` (active venues only, for Pricing) and `clone_tenant_catalog(source, target)`.
- `tenants` holds `slug` (immutable, for URLs/QR codes), `currency`, `timezone`, `status` (`ACTIVE`, `SUSPENDED`, `TEMPLATE`) and `price_tick_seconds`. The `demo-template` venue is the source for `POST /api/v1/platform/tenants` with `demoMenu: true`.

`TenantIsolationTest` (Testcontainers PostgreSQL, `./gradlew :inventory-service:test`) covers the filter, RLS under the application role, the platform API and the internal price feed.

The header is trusted as-is, which is acceptable for demos only: tenant selection is not yet connected to trusted identity, and the platform API has no authentication.

OAuth2 client settings for Google and Apple exist in the separate Authentication module, but that module is not integrated into the running Compose architecture. The current Gateway configuration does not define authentication filters for its inventory routes.

## Boundaries and known gaps

- The running architecture is primarily inventory management plus a demonstration price event pipeline. README features such as orders, stock reservations, payment provider webhooks, bonuses, analytics, and business events remain future work.
- Price configuration changes in Inventory reach Pricing within about a minute; there is no event-driven refresh.
- Price state and version counters are local to a Pricing process; a restart loses them. Multiple Pricing replicas would each run the scheduler and could generate competing price streams.
- WebSocket connections are public and not authorized; any client that knows a venue slug can watch its prices. A screen that falls behind its send buffer is disconnected and must reconnect. An unknown venue slug is not rejected, it simply receives nothing.
- Several Kafka/service addresses are configured directly in code or YAML.
- The development compose file uses a fixed PostgreSQL password and publishes debug ports; `docker-compose-prod.yml` takes secrets from `.env` but there is still no TLS or authentication in front of the gateway.

## Main source locations

- `settings.gradle`, `platform/build.gradle`: Gradle modules and shared dependency versions.
- `docker-compose.yml`, `docker-compose-prod.yml`: service topology and local/deployment profiles.
- `api-gateway/src/main/resources/application-*.yml`: routes, discovery, and ports.
- `inventory-service/src/main/java/.../controllers`, `services`, `repositories`, `dao`: Inventory API and persistence.
- `inventory-service/src/main/resources/db/changelog`: schema migrations and seed data.
- `pricing-service/src/main/java/.../scheduler`, `pricing`, `kafka`, `client`: price config refresh, computation, and event production.
- `price-streaming-service/src/main/java/.../kafka`, `websocket`: event consumption and WebSocket broadcasting.
- `admin-ui/src/api`, `pages`, `components`: admin UI API client and management screens.
