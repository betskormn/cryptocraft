--liquibase formatted sql

--changeset roman:003-seed-inventory-ua context:dev runOnChange:true splitStatements:true endDelimiter:;
--comment: seed for bar inventory + drink items; enums aligned (Type.DRINK=2, Unit ids)

-- =========================================
-- BRANDS
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
-- PRODUCTS (bar ingredients)
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
          -- Spirits (ml)
          ('Jack Daniel''s Whiskey', 'Tennessee whiskey', 'jack-daniels.png', 12000, 2, 'Jack Daniel''s'),
          ('Jameson Whiskey', 'Irish whiskey', 'jameson.png', 9000, 2, 'Jameson'),
          ('Absolut Vodka', 'Classic vodka', 'absolut.png', 15000, 2, 'Absolut'),
          ('Beefeater Gin', 'London dry gin', 'beefeater.png', 10000, 2, 'Beefeater'),
          ('Bacardi White Rum', 'White rum', 'bacardi.png', 11000, 2, 'Bacardi'),
          ('Captain Morgan Spiced Rum', 'Spiced rum', 'captain-morgan.png', 8000, 2, 'Captain Morgan'),
          ('Campari', 'Italian bitter', 'campari.png', 6000, 2, 'Campari'),
          ('Martini Rosso Vermouth', 'Sweet red vermouth', 'martini-rosso.png', 5000, 2, 'Martini'),
          ('Martini Bianco Vermouth', 'White vermouth', 'martini-bianco.png', 5000, 2, 'Martini'),

          -- Mixers (ml)
          ('Coca-Cola', 'Classic cola', 'coke.png', 24000, 2, 'Coca-Cola'),
          ('Schweppes Tonic', 'Tonic water', 'tonic.png', 18000, 2, 'Schweppes'),
          ('Schweppes Ginger Ale', 'Ginger ale', 'ginger-ale.png', 12000, 2, 'Schweppes'),
          ('Soda Water', 'Sparkling water', 'soda.png', 20000, 2, 'Schweppes'),

          -- Juices (ml)
          ('Lime Juice', 'Lime juice (fresh or concentrate)', 'lime-juice.png', 7000, 2, 'Monin'),
          ('Lemon Juice', 'Lemon juice (fresh or concentrate)', 'lemon-juice.png', 7000, 2, 'Monin'),

          -- Syrups (ml)
          ('Monin Sugar Syrup', 'Simple syrup', 'sugar-syrup.png', 6000, 2, 'Monin'),
          ('Monin Grenadine Syrup', 'Grenadine', 'grenadine.png', 4000, 2, 'Monin'),
          ('Monin Vanilla Syrup', 'Vanilla syrup', 'vanilla.png', 3500, 2, 'Monin'),

          -- Coffee (g)
          ('Lavazza Coffee Beans', 'Whole beans for espresso', 'lavazza.png', 8000, 4, 'Lavazza'),

          -- Ice/garnishes (g/pcs)
          ('Ice Cubes', 'Ice for drinks', 'ice.png', 5000, 4, 'Schweppes'),
          ('Mint', 'Fresh mint', 'mint.png', 800, 4, 'Monin'),
          ('Lime', 'Lime for wedges', 'lime.png', 120, 7, 'Metro Chef'),
          ('Lemon', 'Lemon for wedges', 'lemon.png', 120, 7, 'Metro Chef'),
          ('Olives', 'Olives for martini', 'olives.png', 200, 7,
           'Martini')) p(name, description, image, quantity_in_stock, unit, brand_name)
         JOIN b ON b.name = p.brand_name
         JOIN t ON true
WHERE NOT EXISTS (SELECT 1
                  FROM products pr
                  WHERE pr.name = p.name
                    AND pr.tenant_id = t.tenant_id);

-- =========================================
-- ITEMS (drinks) - Type.DRINK = 2
-- =========================================
WITH t AS (SELECT tenant_id
           FROM tenants
           WHERE name = 'default'
           LIMIT 1)
INSERT
INTO items(name, recipe, image, type, tenant_id)
SELECT x.name, x.recipe, x.image, x.type, t.tenant_id
FROM (VALUES ('Whiskey-Cola', 'Whiskey + Coca-Cola + ice. Serve in a highball glass.', 'whiskey-cola.png', 2),
             ('Gin and Tonic', 'Gin + tonic + lime wedge + ice.', 'gin-tonic.png', 2),
             ('Cuba Libre', 'White rum + Coca-Cola + lime + ice.', 'cuba-libre.png', 2),
             ('Old Fashioned', 'Whiskey + sugar syrup + ice. Stir.', 'old-fashioned.png', 2),
             ('Negroni', 'Gin + Campari + Martini Rosso + ice.', 'negroni.png', 2),
             ('Vodka Tonic', 'Vodka + tonic + lime + ice.', 'vodka-tonic.png', 2),
             ('Espresso', 'Espresso from whole coffee beans.', 'espresso.png', 2),
             ('Martini with Olives', 'Gin + Martini Bianco + olives.', 'martini.png', 2),
             ('Mojito', 'White rum + lime juice + sugar syrup + mint + soda + ice.', 'mojito.png', 2),
             ('Homemade Lemonade', 'Lemon juice + sugar syrup + soda + ice.', 'lemonade.png', 2),
             ('Ginger Highball', 'Whiskey + ginger ale + ice + lemon wedge.', 'ginger-highball.png',
              2)) x(name, recipe, image, type)
         JOIN t ON true
WHERE NOT EXISTS (SELECT 1
                  FROM items i
                  WHERE i.name = x.name
                    AND i.tenant_id = t.tenant_id);

-- =========================================
-- ITEM_PRODUCTS mapping (drinks)
-- quantity: ml/g/pcs according to the product unit
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
          -- Whiskey-Cola
          ('Whiskey-Cola', 'Jack Daniel''s Whiskey', 50),
          ('Whiskey-Cola', 'Coca-Cola', 150),
          ('Whiskey-Cola', 'Ice Cubes', 120),

          -- Gin and Tonic
          ('Gin and Tonic', 'Beefeater Gin', 50),
          ('Gin and Tonic', 'Schweppes Tonic', 150),
          ('Gin and Tonic', 'Lime', 1),
          ('Gin and Tonic', 'Ice Cubes', 120),

          -- Cuba Libre
          ('Cuba Libre', 'Bacardi White Rum', 50),
          ('Cuba Libre', 'Coca-Cola', 150),
          ('Cuba Libre', 'Lime', 1),
          ('Cuba Libre', 'Ice Cubes', 120),

          -- Old Fashioned
          ('Old Fashioned', 'Jameson Whiskey', 60),
          ('Old Fashioned', 'Monin Sugar Syrup', 10),
          ('Old Fashioned', 'Ice Cubes', 150),

          -- Negroni
          ('Negroni', 'Beefeater Gin', 30),
          ('Negroni', 'Campari', 30),
          ('Negroni', 'Martini Rosso Vermouth', 30),
          ('Negroni', 'Ice Cubes', 140),

          -- Vodka Tonic
          ('Vodka Tonic', 'Absolut Vodka', 50),
          ('Vodka Tonic', 'Schweppes Tonic', 150),
          ('Vodka Tonic', 'Lime', 1),
          ('Vodka Tonic', 'Ice Cubes', 120),

          -- Espresso
          ('Espresso', 'Lavazza Coffee Beans', 18),

          -- Martini
          ('Martini with Olives', 'Beefeater Gin', 60),
          ('Martini with Olives', 'Martini Bianco Vermouth', 10),
          ('Martini with Olives', 'Olives', 2),

          -- Mojito
          ('Mojito', 'Bacardi White Rum', 50),
          ('Mojito', 'Lime Juice', 25),
          ('Mojito', 'Monin Sugar Syrup', 15),
          ('Mojito', 'Mint', 8),
          ('Mojito', 'Soda Water', 100),
          ('Mojito', 'Ice Cubes', 180),

          -- Lemonade
          ('Homemade Lemonade', 'Lemon Juice', 30),
          ('Homemade Lemonade', 'Monin Sugar Syrup', 20),
          ('Homemade Lemonade', 'Soda Water', 200),
          ('Homemade Lemonade', 'Ice Cubes', 180),

          -- Ginger Highball
          ('Ginger Highball', 'Jameson Whiskey', 50),
          ('Ginger Highball', 'Schweppes Ginger Ale', 150),
          ('Ginger Highball', 'Lemon', 1),
          ('Ginger Highball', 'Ice Cubes', 140)) x(item_name, product_name, qty)
         JOIN it ON it.name = x.item_name
         JOIN prod ON prod.name = x.product_name
         JOIN t ON true
ON CONFLICT (item_id, product_id) DO UPDATE
    SET quantity = EXCLUDED.quantity;


--liquibase formatted sql

--changeset roman:003-seed-food-ua context:dev runOnChange:true splitStatements:true endDelimiter:;
--comment: seed for meals + kitchen ingredients; enums aligned (Type.MEAL=1, Unit ids)

-- =========================================
-- BRANDS (kitchen)
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
-- PRODUCTS (kitchen ingredients)
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
          -- Protein / meat (g)
          ('Beef Patty', 'Beef patty ~150 g', 'beef-patty.png', 6000, 4, 'Metro Chef'),
          ('Chicken Breast', 'Chicken breast fillet', 'chicken-breast.png', 8000, 4, 'Metro Chef'),
          ('Bacon', 'Smoked bacon slices', 'bacon.png', 3000, 4, 'Metro Chef'),

          -- Bases (pcs)
          ('Burger Bun', 'Brioche burger bun', 'bun.png', 120, 7, 'Metro Chef'),
          ('Wheat Tortilla', 'Wheat tortilla for wraps', 'tortilla.png', 100, 7, 'Metro Chef'),

          -- Cheese (g)
          ('Cheddar Cheese', 'Cheddar (slices or grated)', 'cheddar.png', 2500, 4, 'Arla'),
          ('Mozzarella', 'Mozzarella cheese', 'mozzarella.png', 2200, 4, 'Arla'),
          ('Parmesan', 'Grated parmesan', 'parmesan.png', 1500, 4, 'Arla'),

          -- Vegetables (g)
          ('Tomato', 'Fresh tomato', 'tomato.png', 6000, 4, 'Local Farm'),
          ('Iceberg Lettuce', 'Iceberg lettuce leaves', 'lettuce.png', 2500, 4, 'Local Farm'),
          ('Onion', 'Yellow onion', 'onion.png', 3000, 4, 'Local Farm'),
          ('Pickles', 'Sliced pickled cucumbers', 'pickles.png', 2000, 4, 'Heinz'),

          -- Sauces (ml)
          ('Ketchup', 'Tomato ketchup', 'ketchup.png', 5000, 2, 'Heinz'),
          ('Mayonnaise', 'Classic mayonnaise', 'mayo.png', 4500, 2, 'Heinz'),
          ('Mustard', 'Table mustard', 'mustard.png', 3500, 2, 'Heinz'),
          ('BBQ Sauce', 'Smoky barbecue sauce', 'bbq.png', 4000, 2, 'Heinz'),

          -- Pasta/pizza (g, ml)
          ('Spaghetti', 'Durum wheat spaghetti', 'spaghetti.png', 9000, 4, 'Barilla'),
          ('Pizza Dough', 'Prepared pizza dough', 'pizza-dough.png', 6000, 4, 'Metro Chef'),
          ('Tomato Sauce', 'Passata for pizza and pasta', 'tomato-sauce.png', 7000, 2, 'Mutti'),
          ('Olive Oil', 'Extra virgin olive oil', 'olive-oil.png', 5000, 2, 'Metro Chef'),

          -- Spices (g)
          ('Salt', 'Table salt', 'salt.png', 5000, 4, 'Metro Chef'),
          ('Black Pepper', 'Ground black pepper', 'pepper.png', 1200, 4, 'Metro Chef'),

          -- Snacks (g)
          ('Frozen French Fries', 'Frozen french fries', 'fries.png', 12000, 4, 'Metro Chef'),

          -- Extras (pcs)
          ('Egg', 'Chicken egg', 'egg.png', 80, 7,
           'Local Farm')) p(name, description, image, quantity_in_stock, unit, brand_name)
         JOIN b ON b.name = p.brand_name
         JOIN t ON true
WHERE NOT EXISTS (SELECT 1
                  FROM products pr
                  WHERE pr.name = p.name
                    AND pr.tenant_id = t.tenant_id);

-- =========================================
-- ITEMS (food) - Type.MEAL = 1
-- =========================================
WITH t AS (SELECT tenant_id
           FROM tenants
           WHERE name = 'default'
           LIMIT 1)
INSERT
INTO items(name, recipe, image, type, tenant_id)
SELECT x.name, x.recipe, x.image, x.type, t.tenant_id
FROM (VALUES ('Classic Cheeseburger',
              'Toast the bun. Sear a ~150 g patty, season with salt and pepper. Melt the cheddar. Assemble with lettuce, tomato, onion, pickles, ketchup and mayonnaise.',
              'cheeseburger.png',
              1),
             ('BBQ Bacon Burger',
              'Toast the bun. Sear the patty and add cheddar. Crisp the bacon. Assemble with lettuce, tomato, onion, pickles, BBQ sauce and mayonnaise.',
              'bbq-bacon-burger.png',
              1),
             ('Chicken Tortilla Wrap',
              'Grill the chicken breast with salt and pepper. Warm the tortilla. Add lettuce, tomato, onion, mayonnaise and mustard. Wrap tightly and cut in half.',
              'chicken-wrap.png',
              1),
             ('French Fries',
              'Fry until golden. Salt and serve hot.',
              'fries.png',
              1),
             ('Pizza Margherita',
              'Stretch the dough, spread tomato sauce and add mozzarella. Bake at high heat until golden. Finish with a drizzle of olive oil.',
              'margherita.png',
              1),
             ('Spaghetti Pomodoro',
              'Cook spaghetti al dente. Simmer tomato sauce with olive oil, salt and pepper. Toss the pasta with the sauce and serve with parmesan.',
              'spaghetti-pomodoro.png',
              1)) x(name, recipe, image, type)
         JOIN t ON true
WHERE NOT EXISTS (SELECT 1
                  FROM items i
                  WHERE i.name = x.name
                    AND i.tenant_id = t.tenant_id);

-- =========================================
-- ITEM_PRODUCTS mapping (food)
-- quantity: g/ml/pcs according to the product unit
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
          -- Classic Cheeseburger
          ('Classic Cheeseburger', 'Burger Bun', 1),
          ('Classic Cheeseburger', 'Beef Patty', 150),
          ('Classic Cheeseburger', 'Cheddar Cheese', 25),
          ('Classic Cheeseburger', 'Iceberg Lettuce', 20),
          ('Classic Cheeseburger', 'Tomato', 30),
          ('Classic Cheeseburger', 'Onion', 10),
          ('Classic Cheeseburger', 'Pickles', 15),
          ('Classic Cheeseburger', 'Ketchup', 15),
          ('Classic Cheeseburger', 'Mayonnaise', 10),
          ('Classic Cheeseburger', 'Salt', 2),
          ('Classic Cheeseburger', 'Black Pepper', 1),

          -- BBQ Bacon Burger
          ('BBQ Bacon Burger', 'Burger Bun', 1),
          ('BBQ Bacon Burger', 'Beef Patty', 150),
          ('BBQ Bacon Burger', 'Cheddar Cheese', 25),
          ('BBQ Bacon Burger', 'Bacon', 30),
          ('BBQ Bacon Burger', 'Iceberg Lettuce', 20),
          ('BBQ Bacon Burger', 'Tomato', 30),
          ('BBQ Bacon Burger', 'Onion', 10),
          ('BBQ Bacon Burger', 'Pickles', 15),
          ('BBQ Bacon Burger', 'BBQ Sauce', 20),
          ('BBQ Bacon Burger', 'Mayonnaise', 10),
          ('BBQ Bacon Burger', 'Salt', 2),
          ('BBQ Bacon Burger', 'Black Pepper', 1),

          -- Chicken wrap
          ('Chicken Tortilla Wrap', 'Wheat Tortilla', 1),
          ('Chicken Tortilla Wrap', 'Chicken Breast', 140),
          ('Chicken Tortilla Wrap', 'Iceberg Lettuce', 30),
          ('Chicken Tortilla Wrap', 'Tomato', 40),
          ('Chicken Tortilla Wrap', 'Onion', 10),
          ('Chicken Tortilla Wrap', 'Mayonnaise', 15),
          ('Chicken Tortilla Wrap', 'Mustard', 8),
          ('Chicken Tortilla Wrap', 'Salt', 2),
          ('Chicken Tortilla Wrap', 'Black Pepper', 1),

          -- French Fries
          ('French Fries', 'Frozen French Fries', 180),
          ('French Fries', 'Salt', 3),

          -- Pizza Margherita
          ('Pizza Margherita', 'Pizza Dough', 250),
          ('Pizza Margherita', 'Tomato Sauce', 90),
          ('Pizza Margherita', 'Mozzarella', 120),
          ('Pizza Margherita', 'Olive Oil', 7),
          ('Pizza Margherita', 'Salt', 2),
          ('Pizza Margherita', 'Black Pepper', 1),

          -- Spaghetti Pomodoro
          ('Spaghetti Pomodoro', 'Spaghetti', 110),
          ('Spaghetti Pomodoro', 'Tomato Sauce', 120),
          ('Spaghetti Pomodoro', 'Olive Oil', 10),
          ('Spaghetti Pomodoro', 'Parmesan', 18),
          ('Spaghetti Pomodoro', 'Salt', 2),
          ('Spaghetti Pomodoro', 'Black Pepper', 1)) x(item_name, product_name, qty)
         JOIN it ON it.name = x.item_name
         JOIN prod ON prod.name = x.product_name
         JOIN t ON true
ON CONFLICT (item_id, product_id) DO UPDATE
    SET quantity = EXCLUDED.quantity;