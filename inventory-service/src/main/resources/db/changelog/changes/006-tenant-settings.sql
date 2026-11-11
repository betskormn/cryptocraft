--liquibase formatted sql

--changeset roman:006-tenant-settings splitStatements:true endDelimiter:;
--comment: venue settings; status ids match TenantStatus (1 ACTIVE, 2 SUSPENDED, 3 TEMPLATE)
ALTER TABLE tenants
    ADD COLUMN slug               VARCHAR(64),
    ADD COLUMN currency           VARCHAR(3)  NOT NULL DEFAULT 'UAH',
    ADD COLUMN timezone           VARCHAR(64) NOT NULL DEFAULT 'Europe/Kyiv',
    ADD COLUMN status             SMALLINT    NOT NULL DEFAULT 1,
    ADD COLUMN price_tick_seconds INT         NOT NULL DEFAULT 30;

UPDATE tenants SET slug = 'default' WHERE name = 'default';
UPDATE tenants SET slug = 'tenant-' || tenant_id WHERE slug IS NULL;
-- keep the current demo speed for the existing venue
UPDATE tenants SET price_tick_seconds = 3 WHERE slug = 'default';

ALTER TABLE tenants ALTER COLUMN slug SET NOT NULL;
ALTER TABLE tenants ADD CONSTRAINT uk_tenants_slug UNIQUE (slug);
ALTER TABLE tenants ADD CONSTRAINT chk_tenants_slug CHECK (slug ~ '^[a-z0-9]+(-[a-z0-9]+)*$');
ALTER TABLE tenants ADD CONSTRAINT chk_tenants_currency CHECK (currency ~ '^[A-Z]{3}$');
ALTER TABLE tenants ADD CONSTRAINT chk_tenants_price_tick CHECK (price_tick_seconds BETWEEN 1 AND 86400);

--changeset roman:006-clone-tenant-catalog splitStatements:false
--comment: copies brands, products, items and recipes between venues; SECURITY DEFINER so it can read the source venue past RLS
CREATE OR REPLACE FUNCTION clone_tenant_catalog(p_source BIGINT, p_target BIGINT)
    RETURNS VOID
    LANGUAGE plpgsql
    SECURITY DEFINER
    SET search_path = public, pg_temp
AS $$
DECLARE
    r        RECORD;
    v_new_id BIGINT;
BEGIN
    IF p_source = p_target THEN
        RAISE EXCEPTION 'Source and target tenant must differ (%)', p_source;
    END IF;
    IF EXISTS (SELECT 1 FROM brands WHERE tenant_id = p_target)
        OR EXISTS (SELECT 1 FROM items WHERE tenant_id = p_target) THEN
        RAISE EXCEPTION 'Tenant % already has a catalog', p_target;
    END IF;

    DROP TABLE IF EXISTS pg_temp.clone_brand_map, pg_temp.clone_product_map, pg_temp.clone_item_map;
    CREATE TEMP TABLE clone_brand_map (old_id BIGINT PRIMARY KEY, new_id BIGINT NOT NULL) ON COMMIT DROP;
    CREATE TEMP TABLE clone_product_map (old_id BIGINT PRIMARY KEY, new_id BIGINT NOT NULL) ON COMMIT DROP;
    CREATE TEMP TABLE clone_item_map (old_id BIGINT PRIMARY KEY, new_id BIGINT NOT NULL) ON COMMIT DROP;

    FOR r IN SELECT brand_id, name FROM brands WHERE tenant_id = p_source ORDER BY brand_id
        LOOP
            INSERT INTO brands (name, tenant_id)
            VALUES (r.name, p_target)
            RETURNING brand_id INTO v_new_id;
            INSERT INTO pg_temp.clone_brand_map VALUES (r.brand_id, v_new_id);
        END LOOP;

    FOR r IN SELECT * FROM products WHERE tenant_id = p_source ORDER BY product_id
        LOOP
            INSERT INTO products (name, description, image, quantity_in_stock, unit, brand_id, tenant_id)
            VALUES (r.name, r.description, r.image, r.quantity_in_stock, r.unit,
                    (SELECT m.new_id FROM pg_temp.clone_brand_map m WHERE m.old_id = r.brand_id),
                    p_target)
            RETURNING product_id INTO v_new_id;
            INSERT INTO pg_temp.clone_product_map VALUES (r.product_id, v_new_id);
        END LOOP;

    FOR r IN SELECT * FROM items WHERE tenant_id = p_source ORDER BY item_id
        LOOP
            INSERT INTO items (name, recipe, image, type, tenant_id, base_price, min_price, max_price)
            VALUES (r.name, r.recipe, r.image, r.type, p_target, r.base_price, r.min_price, r.max_price)
            RETURNING item_id INTO v_new_id;
            INSERT INTO pg_temp.clone_item_map VALUES (r.item_id, v_new_id);
        END LOOP;

    INSERT INTO item_products (item_id, product_id, quantity, tenant_id)
    SELECT im.new_id, pm.new_id, ip.quantity, p_target
    FROM item_products ip
             JOIN pg_temp.clone_item_map im ON im.old_id = ip.item_id
             JOIN pg_temp.clone_product_map pm ON pm.old_id = ip.product_id
    WHERE ip.tenant_id = p_source;
END
$$;

--changeset roman:006-clone-tenant-catalog-grants runOnChange:true splitStatements:true endDelimiter:;
REVOKE ALL ON FUNCTION clone_tenant_catalog(BIGINT, BIGINT) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION clone_tenant_catalog(BIGINT, BIGINT) TO ${app_db_user};

--changeset roman:006-internal-item-prices-active-only splitStatements:false
--comment: suspended venues and the demo template get no live prices
CREATE OR REPLACE FUNCTION internal_item_prices()
    RETURNS TABLE
            (
                item_id    BIGINT,
                tenant_id  BIGINT,
                base_price NUMERIC,
                min_price  NUMERIC,
                max_price  NUMERIC
            )
    LANGUAGE sql
    STABLE
    SECURITY DEFINER
    SET search_path = public
AS $$
    SELECT i.item_id, i.tenant_id, i.base_price, i.min_price, i.max_price
    FROM items i
             JOIN tenants t ON t.tenant_id = i.tenant_id
    WHERE t.status = 1
$$;

--changeset roman:006-demo-template-tenant context:dev splitStatements:true endDelimiter:;
--comment: template venue that new demo venues copy their menu from; starts as a copy of the default venue
INSERT INTO tenants (name, slug, status, price_tick_seconds)
SELECT 'Demo template', 'demo-template', 3, 3
WHERE NOT EXISTS (SELECT 1 FROM tenants WHERE slug = 'demo-template');

SELECT clone_tenant_catalog(
               (SELECT tenant_id FROM tenants WHERE slug = 'default'),
               (SELECT tenant_id FROM tenants WHERE slug = 'demo-template'));

--changeset roman:006-tenants-updated-at-nullable
--comment: align with the other tables and AuditingEntity, where updated_at is nullable
ALTER TABLE tenants ALTER COLUMN updated_at DROP NOT NULL;
