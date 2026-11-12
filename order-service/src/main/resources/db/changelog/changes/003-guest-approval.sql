--liquibase formatted sql

--changeset roman:003-guest-approval splitStatements:true endDelimiter:;
--comment: a new guest's orders wait until a waiter confirms the guest (someone may have photographed the QR code)
-- status: 1 PENDING, 2 APPROVED, 3 REJECTED (GuestStatus)
ALTER TABLE session_participants ADD COLUMN status SMALLINT NOT NULL DEFAULT 1;
ALTER TABLE session_participants ADD COLUMN decided_at TIMESTAMP;
-- guests from before confirmation existed
UPDATE session_participants SET status = 2;

-- when the order went to the kitchen/bar: at once for a confirmed guest, at confirmation otherwise
-- (orders.status 3 AWAITING_APPROVAL: held until then, not on the screens, stock not written off)
ALTER TABLE orders ADD COLUMN sent_at TIMESTAMP;
UPDATE orders SET sent_at = created_at;

--changeset roman:003-order-pending-stock-fn splitStatements:false
--comment: held orders are not written off until the guest is confirmed
CREATE OR REPLACE FUNCTION pending_stock_write_offs(p_older_than_seconds INT)
    RETURNS TABLE
            (
                order_id  UUID,
                tenant_id BIGINT
            )
    LANGUAGE sql
    STABLE
    SECURITY DEFINER
    SET search_path = public
AS $$
    SELECT o.order_id, o.tenant_id
    FROM orders o
    WHERE o.stock_status = 1
      AND o.status = 1
      AND o.created_at < (now() AT TIME ZONE 'UTC') - make_interval(secs => p_older_than_seconds)
    ORDER BY o.created_at
    LIMIT 100
$$;
