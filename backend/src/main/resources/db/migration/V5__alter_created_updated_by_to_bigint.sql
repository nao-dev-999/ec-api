-- created_by / updated_by を VARCHAR(255) から BIGINT に変更
-- AuditorAware が従業員ID（Long）を格納するため

ALTER TABLE product
    ALTER COLUMN created_by TYPE BIGINT USING (created_by::BIGINT),
    ALTER COLUMN updated_by TYPE BIGINT USING (updated_by::BIGINT);

ALTER TABLE customer_order
    ALTER COLUMN created_by TYPE BIGINT USING (created_by::BIGINT),
    ALTER COLUMN updated_by TYPE BIGINT USING (updated_by::BIGINT);

ALTER TABLE customer_order_detail
    ALTER COLUMN created_by TYPE BIGINT USING (created_by::BIGINT),
    ALTER COLUMN updated_by TYPE BIGINT USING (updated_by::BIGINT);

ALTER TABLE customer
    ALTER COLUMN created_by TYPE BIGINT USING (created_by::BIGINT),
    ALTER COLUMN updated_by TYPE BIGINT USING (updated_by::BIGINT);

ALTER TABLE employee
    ALTER COLUMN created_by TYPE BIGINT USING (created_by::BIGINT),
    ALTER COLUMN updated_by TYPE BIGINT USING (updated_by::BIGINT);
