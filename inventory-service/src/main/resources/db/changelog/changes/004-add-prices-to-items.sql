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
             ('Віскі-кола', 120, 80, 180),
             ('Джин-тонік', 130, 90, 200),
             ('Куба Лібрe', 140, 90, 210),
             ('Олд фешн (спрощено)', 160, 110, 240),
             ('Негроні (спрощено)', 170, 120, 260),
             ('Горілка-тонік', 120, 80, 180),
             ('Еспресо', 50, 30, 80),
             ('Мартіні з оливками', 180, 130, 260),
             ('Мохіто (спрощено)', 150, 100, 220),
             ('Лимонад (домашній)', 90, 60, 140),
             ('Джинджер-хайбол', 140, 90, 210)
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
             ('Класичний чізбургер', 180, 140, 260),
             ('Бургер BBQ з беконом', 200, 150, 300),
             ('Курячий рол у тортильї', 160, 120, 240),
             ('Картопля фрі', 90, 60, 140),
             ('Піца Маргарита', 220, 170, 320),
             ('Спагеті Помодоро', 180, 140, 260)
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