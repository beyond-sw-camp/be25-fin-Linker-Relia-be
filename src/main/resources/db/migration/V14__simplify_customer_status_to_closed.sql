ALTER TABLE customers
    DROP CONSTRAINT chk_customers_status;

ALTER TABLE customer_status_history
    DROP CONSTRAINT chk_customer_status_history_before_status;

ALTER TABLE customer_status_history
    DROP CONSTRAINT chk_customer_status_history_after_status;

UPDATE customer_status_history
SET before_status = 'CLOSED'
WHERE before_status IN ('COMPLETED', 'TERMINATED');

UPDATE customer_status_history
SET after_status = 'CLOSED'
WHERE after_status IN ('COMPLETED', 'TERMINATED');

UPDATE customers c
SET c.customer_status = CASE
        WHEN EXISTS (
            SELECT 1
            FROM contracts ct
            WHERE ct.customer_id = c.id
              AND ct.deleted_at IS NULL
              AND ct.contract_status = 'MAINTENANCE'
        ) THEN 'CONTRACTED'
        WHEN EXISTS (
            SELECT 1
            FROM contracts ct
            WHERE ct.customer_id = c.id
              AND ct.deleted_at IS NULL
        ) THEN 'CLOSED'
        ELSE 'PROSPECT'
    END,
    c.updated_at = CURRENT_TIMESTAMP,
    c.updated_by = COALESCE(c.updated_by, c.created_by);

ALTER TABLE customers
    ADD CONSTRAINT chk_customers_status
        CHECK (customer_status IN ('PROSPECT', 'CONTRACTED', 'CLOSED'));

ALTER TABLE customer_status_history
    ADD CONSTRAINT chk_customer_status_history_before_status
        CHECK (before_status IS NULL OR before_status IN ('PROSPECT', 'CONTRACTED', 'CLOSED'));

ALTER TABLE customer_status_history
    ADD CONSTRAINT chk_customer_status_history_after_status
        CHECK (after_status IN ('PROSPECT', 'CONTRACTED', 'CLOSED'));

UPDATE consultations cs
JOIN customers c ON c.id = cs.customer_id
SET cs.next_scheduled_at = NULL,
    cs.updated_at = CURRENT_TIMESTAMP,
    cs.updated_by = COALESCE(cs.updated_by, cs.created_by)
WHERE c.customer_status = 'CLOSED'
  AND cs.next_scheduled_at IS NOT NULL;
