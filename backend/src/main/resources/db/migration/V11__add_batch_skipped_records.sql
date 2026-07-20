-- fault tolerance でスキップされたレコードの記録用（監査目的）。
CREATE TABLE batch_skipped_records (
    id               BIGSERIAL    PRIMARY KEY,
    job_execution_id BIGINT       NOT NULL,
    step_name        VARCHAR(255) NOT NULL,
    order_detail_id  BIGINT,
    error_message    VARCHAR(1000) NOT NULL,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_batch_skipped_records_job_execution_id
    ON batch_skipped_records (job_execution_id);
