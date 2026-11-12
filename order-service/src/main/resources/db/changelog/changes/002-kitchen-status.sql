--liquibase formatted sql

--changeset roman:002-order-line-kitchen-status splitStatements:true endDelimiter:;
--comment: kitchen/bar screen state of each line; a cancelled line is not charged
-- status: 1 NEW, 2 PREPARING, 3 READY, 4 SERVED, 5 CANCELLED (LineStatus)
ALTER TABLE order_lines ADD COLUMN status SMALLINT NOT NULL DEFAULT 1;
ALTER TABLE order_lines ADD COLUMN status_changed_at TIMESTAMP;
ALTER TABLE order_lines ADD COLUMN cancel_reason VARCHAR(255);

-- the screens list only lines still in work, oldest first
CREATE INDEX ix_order_lines_active ON order_lines (tenant_id, item_type, line_id) WHERE status IN (1, 2, 3);
