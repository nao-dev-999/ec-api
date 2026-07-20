CREATE TABLE daily_sales_summary_by_product (
    product_id     BIGINT       NOT NULL REFERENCES product(id),
    sales_date     DATE         NOT NULL,
    total_amount   DECIMAL(19, 2) NOT NULL DEFAULT 0,
    total_quantity INTEGER      NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by     BIGINT       NOT NULL,
    updated_by     BIGINT       NOT NULL,
    version        INT          NOT NULL DEFAULT 0,
    PRIMARY KEY (product_id, sales_date)
);
