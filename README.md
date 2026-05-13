# CryptoCraft Bar — Trading Prices (Microservices)

Система для бару/бар-ресторану з концепцією “біржового” ціноутворення на напої та їжу: ціни змінюються в реальному часі залежно від попиту, відображаються на TV-екранах у закладі та в мобільному застосунку.  
Клієнти можуть “купити” напій у найвигідніший момент. У певні періоди відбувається “обвал ринку”, коли ціни падають до мінімально рентабельних.

---

##  Основні можливості

### Для клієнтів
- Перегляд актуальних цін у реальному часі (WebSocket / SSE).
- Створення замовлення (кошик), додавання/видалення позицій.
- Оплата через **Google Pay / Apple Pay** (через платіжного провайдера).
- Прив’язка до місця (seat/table) як “легкий акаунт” + опційна авторизація Google/Apple.

### Для персоналу (кухня/бар)
- **KDS/BDS UI**: список замовлень для кухні/бару з статусами виконання.

### Для адмінів / інвентар менеджера
- Адмінка для управління брендами, продуктами, item-ами (стравами/напоями), рецептами та залишками.
- Внесення поставок, коригування складу, контроль “low stock”.

### Для бізнесу
- Bonus & Discount: промокоди, акції, знижки, бонуси/кешбек.
- Analytics: статистика продажів, популярність, ефект “обвалів”, аналітика попиту.

---

##  Архітектура (C4 Container summary)

### Edge (north-south)
- **Mobile App / Web UI / KDS-BDS UI / TV UI** → **API Gateway** (HTTPS, REST/JSON)
- **UI** → **Auth Service** (OIDC / OAuth2, JWT) для логіну
- **UI** ← **WebSocket Server** (WSS) для реального часу (ціни/статуси)

### Internal (east-west)
- Service-to-service виклики **напряму** (не через Gateway) там, де це критично:
    - `Order ↔ Inventory`: reserve/commit/release (REST або gRPC)
    - `Payment ↔ Order`: verify payable amount / mark paid (REST або events)
- Подійна взаємодія через **Message Broker**:
    - `OrderConfirmed`, `OrderPaid`, `OrderCancelled`
    - `PriceChanged`
    - `InventoryReserved`, `InventoryCommitted`, `ReservationExpired`

> Gateway використовується як єдина точка входу для клієнтів, а міжсервісні виклики робляться напряму + через події.

---

##  Сервіси

### 1) Inventory Service
**Відповідальність:** інвентаризація продуктів, розрахунок доступності item-ів (страв/напоїв), резервації під кошик.  
**Ключові фічі:**
- 1 item = набір продуктів (рецепт)
- Перевірка доступності: чи вистачає всіх продуктів для item
- Резервації (pending/hold) з TTL для “кошика” (щоб не oversell)
- Endpoint: “на основі продуктів розрахувати всі доступні item-и з кількістю”

### 2) Order Service
**Відповідальність:** кошик/замовлення, ліміти, стан замовлення, фіксація ціни (quote/lock), інтеграція з інвентарем і оплатою.  
**Ключові фічі:**
- Додавання item у кошик → перерахунок доступності **тільки дельти**
- Обмеження максимальної кількості item-ів в ордері (anti-abuse)
- Публікація подій для Pricing/Analytics/Bonus

### 3) Pricing Service
**Відповідальність:** динамічне ціноутворення (market engine).  
**Ключові фічі:**
- Реакція на продажі (події з Order)
- “Market crash” періоди (мінімально рентабельна ціна)
- Публікація `PriceChanged` для WebSocket/Tv/Mobile

### 4) Payment Service
**Відповідальність:** оплата (Google Pay / Apple Pay) через зовнішнього провайдера еквайрингу.  
**Ключові фічі:**
- Старт/підтвердження платежу
- Інтеграція з PSP/Acquirer (mono / LiqPay / Fondy / WayForPay тощо)
- Webhooks від провайдера (успіх/помилка)
- Публікація подій `PaymentSucceeded/Failed`

### 5) Authentication & Authorization Service
**Відповідальність:** OIDC/OAuth2, видача JWT, ролі/доступи (RBAC).  
**Ролі:**
- `Customer`
- `Staff` (кухар/бармен/офіціант)
- `InventoryManager`
- `Admin`

### 6) Bonus & Discount Service
**Відповідальність:** промокоди, знижки, бонуси/кешбек.  
**Інтеграція:**
- Sync: Order → Bonus (calculate discount)
- Async: споживання `OrderPaid` для нарахування/історії

### 7) Analytics Service
**Відповідальність:** збір подій і агрегації для звітів.  
**Інтеграція:**
- Async: споживання подій з Broker (Order*, PriceChanged, Payment*)
- Read-only API для адмінки (дашборди/графіки)

### 8) WebSocket Server
**Відповідальність:** трансляція real-time даних (ціни, статуси) клієнтам та екранам у закладі.  
**Джерела:** події `PriceChanged` (і опційно `OrderStatusChanged`).

### 9) API Gateway
**Відповідальність:** єдина точка входу, маршрутизація, rate limiting, auth filters (JWT), CORS.

---

##  Основні бізнес-флоу

###  Кошик / резервація
1. UI запитує доступні item-и: `Inventory -> GetSellableItems()`
2. UI додає item у кошик: `Order -> addItem()`
3. Order робить `Inventory -> Reserve(orderId, deltaLines, ttl)`
4. Inventory повертає “зарезервовано / недостатньо”
5. При видаленні позиції: `Inventory -> UpdateReservation(delta)`
6. Якщо кошик кинули: `ReservationExpired` → release

###  Оплата (Google Pay / Apple Pay)
1. UI ініціює оплату: `Payment -> startPayment(orderId)`
2. Payment перевіряє суму/стан ордера (sync або snapshot)
3. Payment викликає **зовнішній PSP** (HTTPS) та отримує підтвердження
4. PSP надсилає webhook `PaymentSucceeded/Failed`
5. Payment публікує `PaymentSucceeded/Failed`
6. Order споживає `PaymentSucceeded` → переводить ордер у `PAID` і виконує `Inventory -> Commit(orderId)`
7. Order публікує `OrderPaid` → Bonus/Analytics реагують

###  Ціни в реальному часі
1. Pricing рахує нові ціни (на подіях `OrderConfirmed/OrderPaid`)
2. Pricing публікує `PriceChanged`
3. WebSocket Server транслює зміни на TV/UI

---

## 📬 Події (Event Catalog – short)

### Order events
- `OrderCreated`
- `OrderConfirmed`
- `OrderPaid`
- `OrderCancelled`
- `OrderStatusChanged`

### Inventory events
- `InventoryReserved`
- `InventoryReservationUpdated`
- `ReservationExpired`
- `InventoryCommitted`

### Pricing events
- `PriceChanged`
- `MarketCrashStarted`
- `MarketCrashEnded`

### Payment events
- `PaymentStarted`
- `PaymentSucceeded`
- `PaymentFailed`

> Всі події повинні бути ідемпотентні на стороні споживача (consumer).

---

##  Безпека
- Auth через Google/Apple (OIDC/OAuth2).
- API Gateway валідуює JWT (або через JWKS, або local verification).
- WebSocket може бути public (тільки ціни) або із перевіркою токену при підключенні.
- Рекомендація: rate-limit на створення/оновлення кошика та max items per order.

---

##  Anti-abuse / Limits
- Max items per order (наприклад 30 lines, max qty per line 10)
- Max active reservations per seatId/user
- TTL на reservation (наприклад 5–10 хв)
- Ідемпотентні reserve/commit/release запити

---

##  Технології (приклад)
- Java 21, Spring Boot
- Spring Cloud Gateway
- PostgreSQL
- Liquibase
- Message Broker: Kafka або RabbitMQ
- WebSocket (WSS) / SSE
- Docker / Docker Compose
- React Admin Panel (вже реалізовано)

---


### Передумови
- Docker + Docker Compose
- JDK 21

### Запуск

docker compose up -d
npm run dev