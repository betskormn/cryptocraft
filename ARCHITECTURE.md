# CryptoCraft Architecture

## Overview

CryptoCraft is a microservice-oriented prototype for inventory administration and dynamic menu pricing. The implemented path covers inventory CRUD, scheduled price generation, Kafka-based price events, and WebSocket price broadcasts. The repository's README describes a broader restaurant platform, but order management, reservations, payment processing, bonuses, and analytics are not implemented as services in the current codebase.

## System context

```mermaid
flowchart LR
    Admin["Admin browser<br/>React / Vite"] -->|HTTP /api, session cookie| Gateway["API Gateway<br/>Spring Cloud Gateway<br/>OAuth2 client (BFF)"]
    Admin -->|sign-in page| Identity["Identity Service<br/>Spring Authorization Server"]
    Gateway -->|tokens, /api/v1/me, /api/v1/staff| Identity
    Order -->|client credentials| Identity
    Gateway -->|Eureka discovery| Inventory["Inventory Service<br/>Spring MVC / JPA"]
    Pricing["Pricing Service<br/>scheduler + pricing engine"] -->|HTTP GET /internal/prices| Inventory
    Inventory -->|JDBC + Liquibase| DB[("PostgreSQL")]
    Pricing -->|price-updates| Kafka[("Kafka")]
    Kafka --> Streaming["Price Streaming Service<br/>Kafka consumer"]
    Browser["TV / price clients"] <-->|WebSocket /ws/prices/{venue}| Streaming
    Gateway --> Order["Order Service<br/>Spring MVC / JPA"]
    Order -->|order-events| Kafka
    KDS["Kitchen / bar screens<br/>admin UI /kds/*"] -->|HTTP polling /api/v1/kitchen| Gateway
    Gateway -.-> Eureka[("Eureka Server")]
    Inventory -.-> Eureka
    Identity -.-> Eureka
    Pricing -.-> Eureka
    Streaming -.-> Eureka
```

Eureka is used for registering services and for all gateway routes; the Pricing service's Inventory client also resolves `inventory-service` through Eureka.

## Modules and responsibilities

| Module | Responsibility and key implementation |
|---|---|
| `admin-ui` | React 19 and TypeScript admin application. Provides brand, product, and item screens and a venue switcher (with "New venue" and a link to the venue's TV board); uses Axios for REST requests and TanStack Query for loading, mutations, and cache invalidation. |
| `api-gateway` | Spring Cloud Gateway (WebFlux), listening on port `8765`. Signs staff in at identity-service and keeps their tokens in its session (see "Signing in"). Routes `/api/v1/brands/**`, `/api/v1/products/**`, `/api/v1/items/**`, `/api/v1/platform/tenants/**` and `/api/v1/venues/**` to Inventory, `/api/v1/me/**` and `/api/v1/staff/**` to Identity, tables, visits, orders and `/api/v1/kitchen/**` to Order, and `/ws/prices/**`, `/tv.html` to Price Streaming, all through Eureka; no other paths are public. |
| `eureka-server` | Spring Cloud Netflix Eureka registry, listening on port `8761`. |
| `inventory-service` | Spring MVC REST API on port `8081`; manages brands, products, items, recipes/item-product links, and product stock. Uses JPA/Hibernate, PostgreSQL, and Liquibase. Implements tenant context and PostgreSQL row-level security. |
| `pricing-service` | Spring service on port `8082`. Periodically loads item base/min/max prices from Inventory, calculates new prices, caches the current prices in memory, and publishes `PriceEvent`s to Kafka. |
| `price-streaming-service` | Spring WebFlux service on port `8083`. Consumes Kafka price events and sends each one to the screens of its venue. WebSocket endpoint: `/ws/prices/{venueSlug}`; TV page: `/tv.html?venue={venueSlug}`. |
| `identity-service` | Spring Authorization Server on port `8086` with its own database `identity`. Staff accounts (`users`), their venues and roles (`venue_memberships`), the sign-in page, OAuth2/OpenID Connect endpoints, JWT signing key, and the API `/api/v1/me` (profile, password) and `/api/v1/staff` (a venue's staff). |
| `security-support` | Shared library (auto-configuration) for the Spring MVC services: OAuth2 resource server with identity-service's keys, `VenueRole`, `VenueAccess` (role in the venue of `X-Tenant-Id`), ProblemDetail 401/403. A service only declares `ServiceSecurityRules`. Test fixtures: `TestTokens`. |
| `order-service` | Spring MVC service on port `8084` with its own database `orders`. Tables with QR tokens, visits (table sessions), anonymous guests (`X-Participant-Id`), orders with prices locked from Pricing, per-guest bills, the kitchen/bar queue with line statuses (KDS). Writes off recipes in Inventory after commit (retried if Inventory is down) and publishes `OrderPlaced` and `OrderLineStatusChanged` to Kafka topic `order-events`. |
| `service-support` | Shared library for the Spring MVC services: base ProblemDetail error handler, JPA `AuditingEntity`, JSON serializer for Kafka messages. |
| `tenant-support` | Shared library (auto-configuration) for tenant-scoped services: `X-Tenant-Id` filter, `TenantContext`, RLS-aware `TenantAwareDataSource`, `TenantEntityListener`. A service only provides a `TenantStatusLookup`. |
| `platform` | Gradle Java platform/BOM centralizing Spring Boot, Spring Cloud, PostgreSQL, MapStruct, and related dependency versions. |

## Request and event flows

### Signing in (staff)

1. The admin UI asks `GET /api/v1/me`; without a session the answer is 401, and the UI sends the browser to `/oauth2/authorization/identity` on the gateway (through Vite or nginx, same origin as the UI).
2. The gateway starts an authorization code flow with PKCE as a confidential client (`cryptocraft-gateway`): the browser goes to identity-service's sign-in page (`/login`, the only place a password is typed), then back to `/login/oauth2/code/identity`.
3. The gateway exchanges the code for tokens and keeps them in its WebSession; the browser gets only the HttpOnly `SESSION` cookie. `SessionTokenRelayFilter` adds the access token to every routed call and refreshes it when it has expired; when it cannot be refreshed the session is ended, the service answers 401 and the UI signs in again.
4. The access token (RS256, 10 minutes) carries `venues` (`{"1": "ADMIN", "7": "STAFF"}`), `platform_admin`, `name` and `email`. They are read from the database at every refresh, so a role change or removal reaches an open session within minutes and a disabled account cannot refresh.
5. Each service checks the token with identity-service's public keys (`/oauth2/jwks`) and the role the token gives in the venue of `X-Tenant-Id` (`ServiceSecurityRules` + `VenueAccess`). The gateway itself lets everything through.
6. Sign-out is a form `POST /logout` (with the CSRF token): the gateway ends its session and sends the browser to identity-service's `/connect/logout`, which ends its own and returns to the UI.

Changes made with the session cookie need the `XSRF-TOKEN` cookie echoed as `X-XSRF-TOKEN` (axios does it); requests without a session (guests, TV) or with their own bearer token do not.

| Role in a venue | May use |
|---|---|
| `ADMIN` | everything in the venue: menu, stock, tables, visits, kitchen/bar screens, staff |
| `INVENTORY_MANAGER` | brands, products, items, stock |
| `STAFF` | visits and bills, kitchen/bar screens, reading items and the stop-list |
| platform admin | every venue and the platform API (creating venues) |
| guest (guest token) | ordering and their own bill in that venue |

### Guests at a table

1. The table's QR code opens `/g/{venue id}/{table code}` (the guest page in the admin UI build; the real guest app comes in stage 6). The guest gives a name and joins: `POST /api/v1/table-sessions/join` (open endpoint) joins the table's open visit or opens one.
2. Order Service asks identity-service, with its own client-credentials token, for the guest's token (`POST /api/v1/guest-tokens`, our services only): RS256 like staff tokens, with `guest: true`, `tenant_id`, `participant_id`, `table_session_id`, 12 hours. The page keeps it and sends it as `Authorization: Bearer`; scanning again with it returns the same guest and bill.
3. Ordering (`POST /api/v1/orders`) and the guest's bill (`GET /api/v1/participants/me/bill`) need a guest token of the venue in `X-Tenant-Id`; the guest is the one in the token. A guest token opens nothing of the staff, and a staff token cannot order as a guest. Closing the visit ends the token's use: Order checks the visit on every order.
4. A new guest is `PENDING`: their orders are stored with the price locked at sending, shown on their bill, but held (`AWAITING_APPROVAL`): not on the kitchen/bar screens, no stock write-off, no `OrderPlaced`. The Visits page shows "N to confirm"; a waiter confirms the guest in the table's bill (`.../participants/{id}/approve`) and the held orders go to the kitchen/bar (their waiting time starts then, `orders.sent_at`), or rejects them (`.../reject`): held orders are cancelled ("Guest not confirmed by the staff") and the guest cannot order. The guest's row is locked by both an order and the decision, so no order slips past it.

The guests' menu (`GET /api/v1/items/sellable`) and live prices (`/ws/prices/{slug}`) stay open.

### Service-to-service calls

Order Service calls Inventory's venue API (menu check, recipe write-offs, venue status) with its own token: client credentials from identity-service (client `order-service`, scope `internal`), cached until it expires and independent of who called Order. `/internal/**` endpoints (Pricing's price feed, Order's current prices) are not routed by the gateway and stay network-only.

### Admin inventory requests

1. The React admin UI calls `/api/v1/brands`, `/api/v1/products`, or `/api/v1/items`.
2. In the Vite development server, `/api` (and `/oauth2`, `/login/oauth2`, `/logout`) is proxied to the API Gateway (default `http://localhost:8765`). The UI's Axios interceptor attaches `X-Tenant-Id` of the selected venue; the gateway adds the person's token.
3. The Gateway forwards the request with the access token to the Inventory Service through its configured Eureka route.
4. Inventory checks the token and the role in that venue, its servlet filter establishes tenant context, then its controllers, services, repositories, and JPA entities perform the operation.
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
3. No database connection is held while Inventory and Pricing answer: a short read transaction, the remote calls, then a short write transaction. After commit, asynchronously, Order Service writes the recipes off in Inventory (`POST /api/v1/stock-consumptions`, idempotent by order id): the order is returned `PENDING` and becomes `WRITTEN_OFF` a moment later. If Inventory is down it stays `PENDING` and `StockWriteOffRetrier` retries it; if Inventory refuses it (4xx) it becomes `FAILED` and is not retried.
4. `OrderPlaced` goes to Kafka topic `order-events` (best effort, before the write-off) for demand-based pricing and analytics; nothing consumes it yet.

Bills are per guest (each pays for themselves); staff see the whole table split per guest and close the visit when it is paid.

### Kitchen and bar screens (KDS)

1. The admin UI pages `/kds/kitchen` (MEAL lines) and `/kds/bar` (DRINK lines) reload the station's queue from `GET /api/v1/kitchen/lines?itemType=…` every 5 seconds through the gateway: lines still in work, in the order they were sent, one ticket per order. A reload that brings an order not shown before plays a sound.
2. Polling, not a socket, on purpose: a kitchen does not need an order in the same second, every reload returns the full state from Order Service (the source of truth), and the screen stays a plain HTTP client behind the same authentication as the rest of the API. `OrderLineStatusChanged` still goes to `order-events` for analytics.
3. Staff move a line with `PATCH /api/v1/kitchen/lines/{lineId}/status`: `NEW → PREPARING → READY → SERVED`, back one step to undo a mis-tap, or `CANCELLED` with a reason. Served and cancelled lines leave the screen and cannot change again.
4. A cancelled line stays in the bill, struck out, and is not charged (the order total is recalculated under a row lock; an order with nothing left becomes `CANCELLED`). The stock already written off is not returned.
5. From the screen the staff can also put an item on the stop-list (`PATCH /api/v1/items/{id}/stop-list` in Inventory).

## Data and persistence

Inventory stores the primary relational data in PostgreSQL:

- `brands`: product brands.
- `products`: name, description, image, stock quantity, unit, brand, and tenant.
- `items`: menu items, price configuration (`base_price`, `min_price`, `max_price`), stop-list flag (`stopped`) and menu position (`display_order`).
- `item_products`: recipe links from items to products, with quantities in the product's unit (`NUMERIC(12,3)`, as is `products.quantity_in_stock`).
- `stock_consumptions` / `stock_consumption_lines`: recipe write-offs per order (`POST /api/v1/stock-consumptions`, idempotent by venue + `orderId`). Stock never goes below zero; a missing amount is recorded as `requested` > `consumed` and returned as a shortage instead of rejecting the order.
- `tenants`: tenant records used by tenant-aware entities and policies.

Identity stores in its own database `identity` (no RLS: accounts are not venue data): `users` (email login, BCrypt password hash, `platform_admin`), `venue_memberships` (user, venue id, role), `signing_keys` (the RSA key that signs tokens, kept so tokens survive restarts) and Spring Authorization Server's `oauth2_registered_client`, `oauth2_authorization` (sign-ins and their refresh tokens; expired ones are deleted hourly) and `oauth2_authorization_consent`. OAuth2 clients and their secrets come from configuration and are written on every start; the first platform admin is created from `identity.bootstrap-admin` while there is none.

Liquibase change sets create and evolve the schema, seed sample data and prices, and configure PostgreSQL row-level security for tenant-scoped inventory tables. Pricing's active configuration and current prices are in-memory caches; Kafka carries price events. No persistent order, payment, or price-history store is implemented.

## Technologies and runtime

- **Backend:** Java 21, Gradle, Spring Boot, Spring MVC, Spring WebFlux, Spring Data JPA, Spring Security/OAuth2 configuration, Spring Cloud Gateway, Eureka.
- **Database and messaging:** PostgreSQL, Liquibase, Kafka, Zookeeper (for the configured Kafka broker).
- **Frontend:** React, TypeScript, Vite, Material UI, TanStack Query, Axios.
- **Local orchestration:** Docker Compose, Kafka UI, PostgreSQL volume.

Each service has `application.yml` (shared), `application-local.yml` (services on the host, infrastructure in Docker; the default profile) and `application-prod.yml` (inside the compose network, addresses overridable by environment variables). `docker-compose.yml` is the development stack: every service in a container with the `prod` profile, debug ports, Kafka UI, healthchecks for start order, and Kafka with two listeners (`kafka:29092` for containers, `localhost:9092` for the host). `docker-compose-prod.yml` is the production-like stack: no debug ports, database and Kafka not published, secrets from `.env`, identity-service published for its sign-in page, and the admin UI as a static nginx build that proxies `/api`, `/oauth2`, `/login/oauth2`, `/logout`, `/ws` and `/tv.html` to the gateway.

The gateway exposes only its explicit routes (inventory, order and identity APIs, platform tenants, `/ws/prices/**`, `/tv.html`) plus its own sign-in and sign-out endpoints; the discovery locator is off so internal endpoints such as `/internal/prices` and `/api/v1/stock-consumptions` are not reachable through it. Eureka evicts dead instances within seconds (self-preservation off, 5 s lease renewal, 15 s expiry) and clients refresh the registry every 5 s.

## Security and tenant model

A tenant is a venue. All venues share one schema; every catalog table carries `tenant_id` and is protected by PostgreSQL row-level security (`FORCE ROW LEVEL SECURITY`, one policy per table on `app_current_tenant()`).

- The service connects as `cryptocraft_app` (no superuser, no `BYPASSRLS`); Liquibase migrates as `postgres`. Credentials: `APP_DB_USER` / `APP_DB_PASSWORD`.
- `TenantFilter` takes the tenant from the `X-Tenant-Id` header: missing or malformed → 400, unknown → 404, suspended → 403. `/internal/**`, `/api/v1/platform/**`, `/api/v1/venues`, actuator and OpenAPI paths are not tenant-scoped. It runs after Spring Security, so a caller without a valid token gets 401 first and a staff token only opens the venues it lists (403 otherwise).
- `TenantAwareDataSource` overwrites `app.tenant_id` on every connection checkout; without a tenant the value is empty, so queries see no rows and writes are rejected.
- Cross-tenant work goes through `SECURITY DEFINER` functions: `internal_item_prices()` (active venues only, for Pricing) and `clone_tenant_catalog(source, target)`.
- `tenants` holds `slug` (immutable, for URLs/QR codes), `currency`, `timezone`, `status` (`ACTIVE`, `SUSPENDED`, `TEMPLATE`) and `price_tick_seconds`. The `demo-template` venue is the source for `POST /api/v1/platform/tenants` with `demoMenu: true`.

`TenantIsolationTest` (Testcontainers PostgreSQL, `./gradlew :inventory-service:test`) covers the filter, RLS under the application role, the platform API and the internal price feed.

Staff requests are authenticated (see "Signing in"): the header only selects which of the person's venues they work in. Guest requests carry a guest token of that venue (see "Guests at a table"). `InventorySecurityTest`, the access tests in `OrderServiceIntegrationTest` and `IdentityServiceIntegrationTest` (the full authorization code flow against a real Spring Authorization Server) cover the rules.

## Boundaries and known gaps

- Payments, fiscal receipts, bonuses and analytics from the README remain future work.
- Guests have no accounts: signing in with Google/Apple and linking it to the visit (for bonuses) comes with the guest app (stage 6). The guest page keeps its token in `localStorage`. A printed QR code is static: anyone with a photo of it can join, which is why a waiter confirms every new guest; ordering only from the venue's Wi-Fi is not implemented.
- The gateway keeps sessions (and the tokens in them) in memory: a gateway restart signs everyone out, and a second gateway instance needs a shared session store (Spring Session with Redis). Identity's sign-in page session is in memory too; refresh tokens are in its database.
- The token signing key is stored in the `identity` database as is; production should keep it in a secret store (or encrypt it) and rotate it. There is no password reset, account lock-out after failed attempts, or 2FA yet.
- `/internal/**` endpoints are protected only by not being routed by the gateway (network trust); Order → Inventory already uses client credentials.
- Price Streaming consumes with a fixed consumer group per topic: with two instances each would get only part of the price events, and their screens would miss them. A per-instance group is needed before scaling it.
- Price configuration changes in Inventory reach Pricing within about a minute; there is no event-driven refresh.
- Price state and version counters are local to a Pricing process; a restart loses them. Multiple Pricing replicas would each run the scheduler and could generate competing price streams.
- WebSocket connections are public and not authorized; any client that knows a venue slug can watch its prices. A screen that falls behind its send buffer is disconnected and must reconnect. An unknown venue slug is not rejected, it simply receives nothing.
- Several Kafka/service addresses are configured directly in code or YAML.
- The development compose file uses a fixed PostgreSQL password, development OAuth2 client secrets and a default platform admin password, and publishes debug ports; `docker-compose-prod.yml` takes all of them from `.env` but there is still no TLS in front of the gateway and identity-service (cookies are not `Secure` without it).

## Main source locations

- `settings.gradle`, `platform/build.gradle`: Gradle modules and shared dependency versions.
- `docker-compose.yml`, `docker-compose-prod.yml`: service topology and local/deployment profiles.
- `api-gateway/src/main/resources/application-*.yml`: routes, discovery, and ports; `api-gateway/src/main/java/.../config/SecurityConfig`, `security`: sign-in (BFF), token relay, CSRF, sign-out.
- `identity-service/src/main/java/.../config/SecurityConfig`, `security`: Spring Authorization Server, sign-in page, token claims, signing key; `controllers`, `services`: `/api/v1/me` and `/api/v1/staff`.
- `security-support`: token checks and venue roles shared by Inventory, Order and Identity (`ServiceSecurityRules`, `VenueAccess`); each service's rules are in its `config/SecurityRulesConfig`.
- `inventory-service/src/main/java/.../controllers`, `services`, `repositories`, `dao`: Inventory API and persistence.
- `inventory-service/src/main/resources/db/changelog`: schema migrations and seed data.
- `pricing-service/src/main/java/.../scheduler`, `pricing`, `kafka`, `client`: price config refresh, computation, and event production.
- `price-streaming-service/src/main/java/.../kafka`, `websocket`: event consumption and WebSocket broadcasting.
- `order-service/src/main/java/.../controllers/KitchenController`, `services/impl/KitchenServiceImpl`: the KDS queue and line statuses.
- `order-service/src/main/java/.../services/impl/GuestJoinServiceImpl`, `GuestApprovalServiceImpl`; `identity-service/.../services/impl/GuestTokenServiceImpl`: guests joining by QR code and their confirmation.
- `admin-ui/src/pages/guest`, `api/guest.api.ts`: the guest page of a table's QR code.
- `admin-ui/src/api`, `pages`, `components`: admin UI API client and management screens; `pages/kds` and `hooks/useKitchenFeed.ts` for the kitchen/bar screens; `auth/` for signing in, the venue switcher's venues and what each role sees; `pages/staff` for the venue's staff.
