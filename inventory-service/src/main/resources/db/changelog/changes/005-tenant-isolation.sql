--liquibase formatted sql

--changeset roman:005-app-role runOnChange:true splitStatements:false
--comment: application role without superuser/BYPASSRLS, so RLS policies actually apply to the service
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = '${app_db_user}') THEN
        CREATE ROLE ${app_db_user} LOGIN PASSWORD '${app_db_password}' NOSUPERUSER NOBYPASSRLS NOCREATEROLE NOCREATEDB;
    ELSE
        ALTER ROLE ${app_db_user} LOGIN PASSWORD '${app_db_password}' NOSUPERUSER NOBYPASSRLS NOCREATEROLE NOCREATEDB;
    END IF;
END $$;

--changeset roman:005-app-role-grants runOnChange:true splitStatements:true endDelimiter:;
--comment: DML only for the application role; schema changes stay with the migration user
GRANT USAGE ON SCHEMA public TO ${app_db_user};
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO ${app_db_user};
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO ${app_db_user};
REVOKE ALL ON databasechangelog, databasechangeloglock FROM ${app_db_user};
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO ${app_db_user};
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT USAGE, SELECT ON SEQUENCES TO ${app_db_user};

--changeset roman:005-current-tenant-fn splitStatements:false
--comment: NULL when app.tenant_id is unset or empty, so policies fail closed instead of erroring on ''::bigint
CREATE OR REPLACE FUNCTION app_current_tenant()
    RETURNS BIGINT
    LANGUAGE sql
    STABLE
AS $$
    SELECT NULLIF(current_setting('app.tenant_id', true), '')::bigint
$$;

--changeset roman:005-tenant-policies splitStatements:true endDelimiter:;
--comment: one policy per table on top of app_current_tenant(); FORCE applies RLS to the table owner as well
DROP POLICY IF EXISTS brands_rls_select ON brands;
DROP POLICY IF EXISTS brands_rls_ins ON brands;
DROP POLICY IF EXISTS brands_rls_upd ON brands;
DROP POLICY IF EXISTS brands_rls_del ON brands;
CREATE POLICY brands_tenant_isolation ON brands
    USING (tenant_id = app_current_tenant())
    WITH CHECK (tenant_id = app_current_tenant());
ALTER TABLE brands FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS items_rls_select ON items;
DROP POLICY IF EXISTS items_rls_ins ON items;
DROP POLICY IF EXISTS items_rls_upd ON items;
DROP POLICY IF EXISTS items_rls_del ON items;
CREATE POLICY items_tenant_isolation ON items
    USING (tenant_id = app_current_tenant())
    WITH CHECK (tenant_id = app_current_tenant());
ALTER TABLE items FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS products_rls_select ON products;
DROP POLICY IF EXISTS products_rls_ins ON products;
DROP POLICY IF EXISTS products_rls_upd ON products;
DROP POLICY IF EXISTS products_rls_del ON products;
CREATE POLICY products_tenant_isolation ON products
    USING (tenant_id = app_current_tenant())
    WITH CHECK (tenant_id = app_current_tenant());
ALTER TABLE products FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS item_products_rls_select ON item_products;
DROP POLICY IF EXISTS item_products_rls_ins ON item_products;
DROP POLICY IF EXISTS item_products_rls_upd ON item_products;
DROP POLICY IF EXISTS item_products_rls_del ON item_products;
CREATE POLICY item_products_tenant_isolation ON item_products
    USING (tenant_id = app_current_tenant())
    WITH CHECK (tenant_id = app_current_tenant());
ALTER TABLE item_products FORCE ROW LEVEL SECURITY;

--changeset roman:005-internal-item-prices splitStatements:false
--comment: cross-tenant read for the pricing service; SECURITY DEFINER runs as the migration user and bypasses RLS
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
$$;

--changeset roman:005-internal-item-prices-grants runOnChange:true splitStatements:true endDelimiter:;
REVOKE ALL ON FUNCTION internal_item_prices() FROM PUBLIC;
GRANT EXECUTE ON FUNCTION internal_item_prices() TO ${app_db_user};
