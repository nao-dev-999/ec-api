ALTER TABLE customer_order
    DROP COLUMN customer_name;

ALTER TABLE customer_order
    ALTER COLUMN customer_id SET NOT NULL;
