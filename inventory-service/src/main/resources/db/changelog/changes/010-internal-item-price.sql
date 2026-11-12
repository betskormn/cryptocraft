--liquibase formatted sql

--changeset roman:010-internal-item-price-fn splitStatements:false
--comment: one item's price configuration for the item change event; filtering inside the function reads one row
-- (a SECURITY DEFINER function is not inlined, so a WHERE on internal_item_prices() read every item of every venue)
CREATE OR REPLACE FUNCTION internal_item_price(p_item_id BIGINT)
    RETURNS TABLE
            (
                item_id            BIGINT,
                tenant_id          BIGINT,
                tenant_slug        VARCHAR,
                price_tick_seconds INT,
                name               VARCHAR,
                type               SMALLINT,
                display_order      INT,
                stopped            BOOLEAN,
                base_price         NUMERIC,
                min_price          NUMERIC,
                max_price          NUMERIC
            )
    LANGUAGE sql
    STABLE
    SECURITY DEFINER
    SET search_path = public
AS $$
    SELECT i.item_id, i.tenant_id, t.slug, t.price_tick_seconds, i.name, i.type, i.display_order, i.stopped,
           i.base_price, i.min_price, i.max_price
    FROM items i
             JOIN tenants t ON t.tenant_id = i.tenant_id
    WHERE i.item_id = p_item_id
      AND t.status = 1
$$;

--changeset roman:010-internal-item-price-grants runOnChange:true splitStatements:true endDelimiter:;
REVOKE ALL ON FUNCTION internal_item_price(BIGINT) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION internal_item_price(BIGINT) TO ${app_db_user};
