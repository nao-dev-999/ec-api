-- V26__add_shipping_address.sql
-- 複数配送先住所（住所帳）。顧客は複数件登録し、注文時にいずれかを選択する。
-- customer_id は customer への外部キーの値のみを保持する軽量な関連とし（cart_item等と同様）、
-- 表示に必要な顧客情報はサービス層で別途解決する。

CREATE TABLE shipping_address (
    id             BIGSERIAL PRIMARY KEY,
    customer_id    BIGINT NOT NULL REFERENCES customer(id) ON DELETE CASCADE,
    recipient_name VARCHAR(100) NOT NULL,
    postal_code    VARCHAR(10)  NOT NULL,
    prefecture     VARCHAR(255) NOT NULL,
    city           VARCHAR(255) NOT NULL,
    address_line1  VARCHAR(255) NOT NULL,
    address_line2  VARCHAR(255),
    phone_number   VARCHAR(20)  NOT NULL,
    is_default     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by     BIGINT,
    updated_by     BIGINT,
    version        INT NOT NULL DEFAULT 0
);

CREATE INDEX idx_shipping_address_customer_id ON shipping_address (customer_id);
