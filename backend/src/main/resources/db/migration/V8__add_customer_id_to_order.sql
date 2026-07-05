ALTER TABLE customer_order
    ADD COLUMN customer_id BIGINT REFERENCES customer(id);
