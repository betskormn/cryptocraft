--liquibase formatted sql

--changeset roman:004-add-prices-to-items context:dev runOnChange:true splitStatements:true endDelimiter:;
--comment: add pricing fields and seed default prices for items

-- =========================================
-- ADD COLUMNS
-- =========================================

ALTER TABLE items
    ADD COLUMN base_price NUMERIC(10,2) NOT NULL DEFAULT 0;

ALTER TABLE items
    ADD COLUMN min_price NUMERIC(10,2) NOT NULL DEFAULT 0;

ALTER TABLE items
    ADD COLUMN max_price NUMERIC(10,2) NOT NULL DEFAULT 0;

-- =========================================
-- SEED PRICES FOR DRINKS
-- =========================================

WITH t AS (
    SELECT tenant_id FROM tenants WHERE name = 'default' LIMIT 1
)
UPDATE items i
SET base_price = x.base_price,
    min_price  = x.min_price,
    max_price  = x.max_price
FROM (
         VALUES
             ('Whiskey-Cola', 120, 80, 180),
             ('Gin and Tonic', 130, 90, 200),
             ('Cuba Libre', 140, 90, 210),
             ('Old Fashioned', 160, 110, 240),
             ('Negroni', 170, 120, 260),
             ('Vodka Tonic', 120, 80, 180),
             ('Espresso', 50, 30, 80),
             ('Martini with Olives', 180, 130, 260),
             ('Mojito', 150, 100, 220),
             ('Homemade Lemonade', 90, 60, 140),
             ('Ginger Highball', 140, 90, 210)
     ) AS x(name, base_price, min_price, max_price)
         JOIN t ON true
WHERE i.name = x.name
  AND i.tenant_id = t.tenant_id;

-- =========================================
-- SEED PRICES FOR FOOD
-- =========================================

WITH t AS (
    SELECT tenant_id FROM tenants WHERE name = 'default' LIMIT 1
)
UPDATE items i
SET base_price = x.base_price,
    min_price  = x.min_price,
    max_price  = x.max_price
FROM (
         VALUES
             ('Classic Cheeseburger', 180, 140, 260),
             ('BBQ Bacon Burger', 200, 150, 300),
             ('Chicken Tortilla Wrap', 160, 120, 240),
             ('French Fries', 90, 60, 140),
             ('Pizza Margherita', 220, 170, 320),
             ('Spaghetti Pomodoro', 180, 140, 260)
     ) AS x(name, base_price, min_price, max_price)
         JOIN t ON true
WHERE i.name = x.name
  AND i.tenant_id = t.tenant_id;

-- =========================================
-- CONSTRAINT: PRICE VALIDATION
-- =========================================

ALTER TABLE items
    ADD CONSTRAINT chk_item_price_range
        CHECK (min_price <= base_price AND base_price <= max_price);