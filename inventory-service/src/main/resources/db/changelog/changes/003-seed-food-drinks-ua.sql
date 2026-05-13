--liquibase formatted sql

--changeset roman:003-seed-inventory-ua context:dev runOnChange:true splitStatements:true endDelimiter:;
--comment: UA seed for bar inventory + drink items; enums aligned (Type.DRINK=2, Unit ids)

-- =========================================
-- BRANDS (бренди залишаємо як є / англійською)
-- =========================================
INSERT INTO tenants (name)
VALUES ('default')
ON CONFLICT (name) DO NOTHING;

-- ================================
-- INVENTORY + DRINKS
-- ================================

--changeset roman:002-seed-inventory-ua context:dev runOnChange:true splitStatements:true endDelimiter:;

-- BRANDS
WITH t AS (SELECT tenant_id
           FROM tenants
           WHERE name = 'default'
           LIMIT 1)
INSERT
INTO brands(name, tenant_id)
SELECT v.name, t.tenant_id
FROM (VALUES ('Jack Daniel''s'),
             ('Jameson'),
             ('Bacardi'),
             ('Captain Morgan'),
             ('Absolut'),
             ('Beefeater'),
             ('Campari'),
             ('Martini'),
             ('Coca-Cola'),
             ('Schweppes'),
             ('Monin'),
             ('Lavazza'),
             ('Metro Chef')) v(name),
     t
ON CONFLICT (tenant_id, name) DO NOTHING;

-- =========================================
-- PRODUCTS (барні інгредієнти) - українські назви
-- Unit:
--   MILLILITERS=2, GRAMS=4, PIECE=7
-- =========================================
WITH t AS (SELECT tenant_id
           FROM tenants
           WHERE name = 'default'
           LIMIT 1),
     b AS (SELECT brand_id, name
           FROM brands
           WHERE tenant_id = (SELECT tenant_id FROM t))
INSERT
INTO products(name, description, image, quantity_in_stock, unit, brand_id, tenant_id)
SELECT p.name, p.description, p.image, p.quantity_in_stock, p.unit, b.brand_id, t.tenant_id
FROM (VALUES
          -- Міцний алкоголь (мл)
          ('Віскі Jack Daniel''s', 'Теннессі-віскі', 'jack-daniels.png', 12000, 2, 'Jack Daniel''s'),
          ('Віскі Jameson', 'Ірландське віскі', 'jameson.png', 9000, 2, 'Jameson'),
          ('Горілка Absolut', 'Класична горілка', 'absolut.png', 15000, 2, 'Absolut'),
          ('Джин Beefeater', 'Лондонський сухий джин', 'beefeater.png', 10000, 2, 'Beefeater'),
          ('Ром Bacardi білий', 'Білий ром', 'bacardi.png', 11000, 2, 'Bacardi'),
          ('Ром Captain Morgan пряний', 'Пряний ром', 'captain-morgan.png', 8000, 2, 'Captain Morgan'),
          ('Campari', 'Італійський біттер', 'campari.png', 6000, 2, 'Campari'),
          ('Вермут Martini Rosso', 'Червоний солодкий вермут', 'martini-rosso.png', 5000, 2, 'Martini'),
          ('Вермут Martini Bianco', 'Білий вермут', 'martini-bianco.png', 5000, 2, 'Martini'),

          -- Міксери (мл)
          ('Coca-Cola', 'Класична кола', 'coke.png', 24000, 2, 'Coca-Cola'),
          ('Schweppes Tonic', 'Тонік', 'tonic.png', 18000, 2, 'Schweppes'),
          ('Schweppes Ginger Ale', 'Джинджер ель', 'ginger-ale.png', 12000, 2, 'Schweppes'),
          ('Содова', 'Газована вода', 'soda.png', 20000, 2, 'Schweppes'),

          -- Соки/фреші (мл)
          ('Сік лайма', 'Сік лайма (фреш/концентрат)', 'lime-juice.png', 7000, 2, 'Monin'),
          ('Сік лимона', 'Сік лимона (фреш/концентрат)', 'lemon-juice.png', 7000, 2, 'Monin'),

          -- Сиропи (мл)
          ('Сироп цукровий Monin', 'Цукровий сироп (simple syrup)', 'sugar-syrup.png', 6000, 2, 'Monin'),
          ('Сироп гренадін Monin', 'Гренадін', 'grenadine.png', 4000, 2, 'Monin'),
          ('Сироп ванільний Monin', 'Ванільний сироп', 'vanilla.png', 3500, 2, 'Monin'),

          -- Кава (г)
          ('Кава Lavazza (зерна)', 'Зернова кава для еспресо', 'lavazza.png', 8000, 4, 'Lavazza'),

          -- Лід/гарніші (г/шт)
          ('Лід кубики', 'Лід для напоїв', 'ice.png', 5000, 4, 'Schweppes'),
          ('М''ята', 'Свіжа м''ята', 'mint.png', 800, 4, 'Monin'),
          ('Лайм', 'Лайм для часточок', 'lime.png', 120, 7, 'Metro Chef'),
          ('Лимон', 'Лимон для часточок', 'lemon.png', 120, 7, 'Metro Chef'),
          ('Оливки', 'Оливки для мартіні', 'olives.png', 200, 7,
           'Martini')) p(name, description, image, quantity_in_stock, unit, brand_name)
         JOIN b ON b.name = p.brand_name
         JOIN t ON true
WHERE NOT EXISTS (SELECT 1
                  FROM products pr
                  WHERE pr.name = p.name
                    AND pr.tenant_id = t.tenant_id);

-- =========================================
-- ITEMS (напої) - Type.DRINK = 2
-- =========================================
WITH t AS (SELECT tenant_id
           FROM tenants
           WHERE name = 'default'
           LIMIT 1)
INSERT
INTO items(name, recipe, image, type, tenant_id)
SELECT x.name, x.recipe, x.image, x.type, t.tenant_id
FROM (VALUES ('Віскі-кола', 'Віскі + Coca-Cola + лід. Подати у хайболі.', 'whiskey-cola.png', 2),
             ('Джин-тонік', 'Джин + тонік + часточка лайма + лід.', 'gin-tonic.png', 2),
             ('Куба Лібрe', 'Білий ром + Coca-Cola + лайм + лід.', 'cuba-libre.png', 2),
             ('Олд фешн (спрощено)', 'Віскі + цукровий сироп + лід. Перемішати.', 'old-fashioned.png', 2),
             ('Негроні (спрощено)', 'Джин + Campari + Martini Rosso + лід.', 'negroni.png', 2),
             ('Горілка-тонік', 'Горілка + тонік + лайм + лід.', 'vodka-tonic.png', 2),
             ('Еспресо', 'Еспресо із зернової кави.', 'espresso.png', 2),
             ('Мартіні з оливками', 'Джин + Martini Bianco + оливки.', 'martini.png', 2),
             ('Мохіто (спрощено)', 'Білий ром + сік лайма + цукровий сироп + м''ята + содова + лід.', 'mojito.png', 2),
             ('Лимонад (домашній)', 'Сік лимона + цукровий сироп + содова + лід.', 'lemonade.png', 2),
             ('Джинджер-хайбол', 'Віскі + ginger ale + лід + часточка лимона.', 'ginger-highball.png',
              2)) x(name, recipe, image, type)
         JOIN t ON true
WHERE NOT EXISTS (SELECT 1
                  FROM items i
                  WHERE i.name = x.name
                    AND i.tenant_id = t.tenant_id);

-- =========================================
-- ITEM_PRODUCTS mapping (напої)
-- quantity: мл/г/шт відповідно до unit продукту
-- =========================================
WITH t AS (SELECT tenant_id
           FROM tenants
           WHERE name = 'default'
           LIMIT 1),
     prod AS (SELECT product_id, name
              FROM products
              WHERE tenant_id = (SELECT tenant_id FROM t)),
     it AS (SELECT item_id, name
            FROM items
            WHERE tenant_id = (SELECT tenant_id FROM t))
INSERT
INTO item_products(item_id, product_id, quantity, tenant_id)
SELECT it.item_id, prod.product_id, x.qty, t.tenant_id
FROM (VALUES
          -- Віскі-кола
          ('Віскі-кола', 'Віскі Jack Daniel''s', 50),
          ('Віскі-кола', 'Coca-Cola', 150),
          ('Віскі-кола', 'Лід кубики', 120),

          -- Джин-тонік
          ('Джин-тонік', 'Джин Beefeater', 50),
          ('Джин-тонік', 'Schweppes Tonic', 150),
          ('Джин-тонік', 'Лайм', 1),
          ('Джин-тонік', 'Лід кубики', 120),

          -- Куба Лібрe
          ('Куба Лібрe', 'Ром Bacardi білий', 50),
          ('Куба Лібрe', 'Coca-Cola', 150),
          ('Куба Лібрe', 'Лайм', 1),
          ('Куба Лібрe', 'Лід кубики', 120),

          -- Олд фешн
          ('Олд фешн (спрощено)', 'Віскі Jameson', 60),
          ('Олд фешн (спрощено)', 'Сироп цукровий Monin', 10),
          ('Олд фешн (спрощено)', 'Лід кубики', 150),

          -- Негроні
          ('Негроні (спрощено)', 'Джин Beefeater', 30),
          ('Негроні (спрощено)', 'Campari', 30),
          ('Негроні (спрощено)', 'Вермут Martini Rosso', 30),
          ('Негроні (спрощено)', 'Лід кубики', 140),

          -- Горілка-тонік
          ('Горілка-тонік', 'Горілка Absolut', 50),
          ('Горілка-тонік', 'Schweppes Tonic', 150),
          ('Горілка-тонік', 'Лайм', 1),
          ('Горілка-тонік', 'Лід кубики', 120),

          -- Еспресо
          ('Еспресо', 'Кава Lavazza (зерна)', 18),

          -- Мартіні
          ('Мартіні з оливками', 'Джин Beefeater', 60),
          ('Мартіні з оливками', 'Вермут Martini Bianco', 10),
          ('Мартіні з оливками', 'Оливки', 2),

          -- Мохіто
          ('Мохіто (спрощено)', 'Ром Bacardi білий', 50),
          ('Мохіто (спрощено)', 'Сік лайма', 25),
          ('Мохіто (спрощено)', 'Сироп цукровий Monin', 15),
          ('Мохіто (спрощено)', 'М''ята', 8),
          ('Мохіто (спрощено)', 'Содова', 100),
          ('Мохіто (спрощено)', 'Лід кубики', 180),

          -- Лимонад
          ('Лимонад (домашній)', 'Сік лимона', 30),
          ('Лимонад (домашній)', 'Сироп цукровий Monin', 20),
          ('Лимонад (домашній)', 'Содова', 200),
          ('Лимонад (домашній)', 'Лід кубики', 180),

          -- Джинджер-хайбол
          ('Джинджер-хайбол', 'Віскі Jameson', 50),
          ('Джинджер-хайбол', 'Schweppes Ginger Ale', 150),
          ('Джинджер-хайбол', 'Лимон', 1),
          ('Джинджер-хайбол', 'Лід кубики', 140)) x(item_name, product_name, qty)
         JOIN it ON it.name = x.item_name
         JOIN prod ON prod.name = x.product_name
         JOIN t ON true
ON CONFLICT (item_id, product_id) DO UPDATE
    SET quantity = EXCLUDED.quantity;


--liquibase formatted sql

--changeset roman:003-seed-food-ua context:dev runOnChange:true splitStatements:true endDelimiter:;
--comment: UA seed for meals + kitchen ingredients; enums aligned (Type.MEAL=1, Unit ids)

-- =========================================
-- BRANDS (кухня) - бренди англійською ок
-- =========================================
WITH t AS (SELECT tenant_id
           FROM tenants
           WHERE name = 'default'
           LIMIT 1)
INSERT
INTO brands(name, tenant_id)
SELECT v.name, t.tenant_id
FROM (VALUES ('Local Farm'),
             ('Heinz'),
             ('Barilla'),
             ('Arla'),
             ('Metro Chef'),
             ('Mutti')) v(name),
     t
ON CONFLICT (tenant_id, name) DO NOTHING;

-- =========================================
-- PRODUCTS (кухонні інгредієнти) - українські назви
-- Unit:
--   MILLILITERS=2, GRAMS=4, PIECE=7
-- =========================================
WITH t AS (SELECT tenant_id
           FROM tenants
           WHERE name = 'default'
           LIMIT 1),
     b AS (SELECT brand_id, name
           FROM brands
           WHERE tenant_id = (SELECT tenant_id FROM t))
INSERT
INTO products(name, description, image, quantity_in_stock, unit, brand_id, tenant_id)
SELECT p.name, p.description, p.image, p.quantity_in_stock, p.unit, b.brand_id, t.tenant_id
FROM (VALUES
          -- Білки / м'ясо (г)
          ('Котлета яловича', 'Яловича котлета ~150 г', 'beef-patty.png', 6000, 4, 'Metro Chef'),
          ('Куряче філе', 'Філе курячої грудки', 'chicken-breast.png', 8000, 4, 'Metro Chef'),
          ('Бекон', 'Копчений бекон скибками', 'bacon.png', 3000, 4, 'Metro Chef'),

          -- Основи (шт)
          ('Булочка для бургера', 'Бріош для бургера', 'bun.png', 120, 7, 'Metro Chef'),
          ('Тортилья пшенична', 'Пшенична тортилья для ролів', 'tortilla.png', 100, 7, 'Metro Chef'),

          -- Сир (г)
          ('Сир чедер', 'Сир Чедер (скибки/тертий)', 'cheddar.png', 2500, 4, 'Arla'),
          ('Сир моцарела', 'Моцарела', 'mozzarella.png', 2200, 4, 'Arla'),
          ('Сир пармезан', 'Пармезан (тертий)', 'parmesan.png', 1500, 4, 'Arla'),

          -- Овочі (г)
          ('Помідор', 'Свіжий помідор', 'tomato.png', 6000, 4, 'Local Farm'),
          ('Салат айсберг', 'Листя салату Айсберг', 'lettuce.png', 2500, 4, 'Local Farm'),
          ('Цибуля ріпчаста', 'Ріпчаста цибуля', 'onion.png', 3000, 4, 'Local Farm'),
          ('Огірки мариновані', 'Мариновані огірки (скибочки)', 'pickles.png', 2000, 4, 'Heinz'),

          -- Соуси (мл)
          ('Кетчуп', 'Томатний кетчуп', 'ketchup.png', 5000, 2, 'Heinz'),
          ('Майонез', 'Класичний майонез', 'mayo.png', 4500, 2, 'Heinz'),
          ('Гірчиця', 'Гірчиця столова', 'mustard.png', 3500, 2, 'Heinz'),
          ('Соус BBQ', 'Копчений соус барбекю', 'bbq.png', 4000, 2, 'Heinz'),

          -- Паста/піца (г, мл)
          ('Паста (спагеті)', 'Спагеті з твердих сортів пшениці', 'spaghetti.png', 9000, 4, 'Barilla'),
          ('Тісто для піци', 'Підготовлене тісто для піци', 'pizza-dough.png', 6000, 4, 'Metro Chef'),
          ('Томатний соус', 'Пасата/соус для піци та пасти', 'tomato-sauce.png', 7000, 2, 'Mutti'),
          ('Оливкова олія', 'Оливкова олія extra virgin', 'olive-oil.png', 5000, 2, 'Metro Chef'),

          -- Спеції (г)
          ('Сіль', 'Сіль кухонна', 'salt.png', 5000, 4, 'Metro Chef'),
          ('Перець чорний', 'Мелений чорний перець', 'pepper.png', 1200, 4, 'Metro Chef'),

          -- Снеки (г)
          ('Картопля фрі (заморожена)', 'Заморожена картопля фрі', 'fries.png', 12000, 4, 'Metro Chef'),

          -- Додатково (шт)
          ('Яйце куряче', 'Куряче яйце', 'egg.png', 80, 7,
           'Local Farm')) p(name, description, image, quantity_in_stock, unit, brand_name)
         JOIN b ON b.name = p.brand_name
         JOIN t ON true
WHERE NOT EXISTS (SELECT 1
                  FROM products pr
                  WHERE pr.name = p.name
                    AND pr.tenant_id = t.tenant_id);

-- =========================================
-- ITEMS (їжа) - Type.MEAL = 1
-- =========================================
WITH t AS (SELECT tenant_id
           FROM tenants
           WHERE name = 'default'
           LIMIT 1)
INSERT
INTO items(name, recipe, image, type, tenant_id)
SELECT x.name, x.recipe, x.image, x.type, t.tenant_id
FROM (VALUES ('Класичний чізбургер',
              'Підсмажити булочку. Обсмажити котлету ~150 г, посолити/поперчити. Розплавити чедер. Зібрати з салатом, помідором, цибулею, маринованими огірками, кетчупом і майонезом.',
              'cheeseburger.png',
              1),
             ('Бургер BBQ з беконом',
              'Підсмажити булочку. Обсмажити котлету, додати чедер. Підрум''янити бекон. Зібрати з салатом, помідором, цибулею, огірками, соусом BBQ та майонезом.',
              'bbq-bacon-burger.png',
              1),
             ('Курячий рол у тортильї',
              'Обсмажити/пригрилити куряче філе з сіллю та перцем. Підігріти тортилью. Додати салат, помідор, цибулю, майонез і гірчицю. Загорнути щільно та розрізати.',
              'chicken-wrap.png',
              1),
             ('Картопля фрі',
              'Обсмажити до золотистої скоринки. Посолити. Подавати гарячою.',
              'fries.png',
              1),
             ('Піца Маргарита',
              'Розтягнути тісто, нанести томатний соус, додати моцарелу. Випікати на високій температурі до рум''яності. Завершити краплею оливкової олії.',
              'margherita.png',
              1),
             ('Спагеті Помодоро',
              'Відварити спагеті аль денте. Протушкувати томатний соус з оливковою олією, сіллю та перцем. Змішати пасту з соусом, подати з пармезаном.',
              'spaghetti-pomodoro.png',
              1)) x(name, recipe, image, type)
         JOIN t ON true
WHERE NOT EXISTS (SELECT 1
                  FROM items i
                  WHERE i.name = x.name
                    AND i.tenant_id = t.tenant_id);

-- =========================================
-- ITEM_PRODUCTS mapping (їжа)
-- quantity: г/мл/шт відповідно до unit продукту
-- =========================================
WITH t AS (SELECT tenant_id
           FROM tenants
           WHERE name = 'default'
           LIMIT 1),
     prod AS (SELECT product_id, name
              FROM products
              WHERE tenant_id = (SELECT tenant_id FROM t)),
     it AS (SELECT item_id, name
            FROM items
            WHERE tenant_id = (SELECT tenant_id FROM t))
INSERT
INTO item_products(item_id, product_id, quantity, tenant_id)
SELECT it.item_id, prod.product_id, x.qty, t.tenant_id
FROM (VALUES
          -- Класичний чізбургер
          ('Класичний чізбургер', 'Булочка для бургера', 1),
          ('Класичний чізбургер', 'Котлета яловича', 150),
          ('Класичний чізбургер', 'Сир чедер', 25),
          ('Класичний чізбургер', 'Салат айсберг', 20),
          ('Класичний чізбургер', 'Помідор', 30),
          ('Класичний чізбургер', 'Цибуля ріпчаста', 10),
          ('Класичний чізбургер', 'Огірки мариновані', 15),
          ('Класичний чізбургер', 'Кетчуп', 15),
          ('Класичний чізбургер', 'Майонез', 10),
          ('Класичний чізбургер', 'Сіль', 2),
          ('Класичний чізбургер', 'Перець чорний', 1),

          -- Бургер BBQ з беконом
          ('Бургер BBQ з беконом', 'Булочка для бургера', 1),
          ('Бургер BBQ з беконом', 'Котлета яловича', 150),
          ('Бургер BBQ з беконом', 'Сир чедер', 25),
          ('Бургер BBQ з беконом', 'Бекон', 30),
          ('Бургер BBQ з беконом', 'Салат айсберг', 20),
          ('Бургер BBQ з беконом', 'Помідор', 30),
          ('Бургер BBQ з беконом', 'Цибуля ріпчаста', 10),
          ('Бургер BBQ з беконом', 'Огірки мариновані', 15),
          ('Бургер BBQ з беконом', 'Соус BBQ', 20),
          ('Бургер BBQ з беконом', 'Майонез', 10),
          ('Бургер BBQ з беконом', 'Сіль', 2),
          ('Бургер BBQ з беконом', 'Перець чорний', 1),

          -- Курячий рол
          ('Курячий рол у тортильї', 'Тортилья пшенична', 1),
          ('Курячий рол у тортильї', 'Куряче філе', 140),
          ('Курячий рол у тортильї', 'Салат айсберг', 30),
          ('Курячий рол у тортильї', 'Помідор', 40),
          ('Курячий рол у тортильї', 'Цибуля ріпчаста', 10),
          ('Курячий рол у тортильї', 'Майонез', 15),
          ('Курячий рол у тортильї', 'Гірчиця', 8),
          ('Курячий рол у тортильї', 'Сіль', 2),
          ('Курячий рол у тортильї', 'Перець чорний', 1),

          -- Картопля фрі
          ('Картопля фрі', 'Картопля фрі (заморожена)', 180),
          ('Картопля фрі', 'Сіль', 3),

          -- Піца Маргарита
          ('Піца Маргарита', 'Тісто для піци', 250),
          ('Піца Маргарита', 'Томатний соус', 90),
          ('Піца Маргарита', 'Сир моцарела', 120),
          ('Піца Маргарита', 'Оливкова олія', 7),
          ('Піца Маргарита', 'Сіль', 2),
          ('Піца Маргарита', 'Перець чорний', 1),

          -- Спагеті Помодоро
          ('Спагеті Помодоро', 'Паста (спагеті)', 110),
          ('Спагеті Помодоро', 'Томатний соус', 120),
          ('Спагеті Помодоро', 'Оливкова олія', 10),
          ('Спагеті Помодоро', 'Сир пармезан', 18),
          ('Спагеті Помодоро', 'Сіль', 2),
          ('Спагеті Помодоро', 'Перець чорний', 1)) x(item_name, product_name, qty)
         JOIN it ON it.name = x.item_name
         JOIN prod ON prod.name = x.product_name
         JOIN t ON true
ON CONFLICT (item_id, product_id) DO UPDATE
    SET quantity = EXCLUDED.quantity;