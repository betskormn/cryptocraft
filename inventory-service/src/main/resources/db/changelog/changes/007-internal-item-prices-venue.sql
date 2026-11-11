--liquibase formatted sql

--changeset roman:007-internal-item-prices-venue splitStatements:true endDelimiter:;
--comment: pricing needs the venue slug and tick, and item name/type for the TV screens; the return type changes, so drop and recreate
DROP FUNCTION IF EXISTS internal_item_prices();

--changeset roman:007-internal-item-prices-venue-fn splitStatements:false
CREATE FUNCTION internal_item_prices()
    RETURNS TABLE
            (
                item_id            BIGINT,
                tenant_id          BIGINT,
                tenant_slug        VARCHAR,
                price_tick_seconds INT,
                name               VARCHAR,
                type               SMALLINT,
                base_price         NUMERIC,
                min_price          NUMERIC,
                max_price          NUMERIC
            )
    LANGUAGE sql
    STABLE
    SECURITY DEFINER
    SET search_path = public
AS $$
    SELECT i.item_id, i.tenant_id, t.slug, t.price_tick_seconds, i.name, i.type,
           i.base_price, i.min_price, i.max_price
    FROM items i
             JOIN tenants t ON t.tenant_id = i.tenant_id
    WHERE t.status = 1
$$;

--changeset roman:007-internal-item-prices-venue-grants runOnChange:true splitStatements:true endDelimiter:;
REVOKE ALL ON FUNCTION internal_item_prices() FROM PUBLIC;
GRANT EXECUTE ON FUNCTION internal_item_prices() TO ${app_db_user};
