CREATE TABLE cart_item (
    id          BIGSERIAL PRIMARY KEY,
    customer_id BIGINT  NOT NULL REFERENCES customer(id) ON DELETE CASCADE,
    product_id  BIGINT  NOT NULL REFERENCES product(id) ON DELETE CASCADE,
    quantity    INTEGER NOT NULL CHECK (quantity > 0),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at  TIMESTAMPTZ,
    created_by  BIGINT,
    updated_by  BIGINT,
    deleted_by  VARCHAR(255),
    version     INT NOT NULL DEFAULT 0,
    UNIQUE (customer_id, product_id)
);
