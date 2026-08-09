-- V22__add_coupon.sql
-- クーポン（定額割引、コード入力式）機能。
-- customer_order.coupon_code は coupon.code への外部キーにしない
-- （クーポンが削除・変更されても過去の注文の適用結果を保持するため）。

CREATE TABLE coupon (
    id              BIGSERIAL PRIMARY KEY,
    code            VARCHAR(30) NOT NULL UNIQUE,
    discount_amount NUMERIC(10, 2) NOT NULL CHECK (discount_amount > 0),
    valid_from      TIMESTAMPTZ,
    valid_to        TIMESTAMPTZ,
    usage_limit     INT,
    usage_count     INT NOT NULL DEFAULT 0,
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      BIGINT,
    updated_by      BIGINT,
    version         INT NOT NULL DEFAULT 0
);

ALTER TABLE customer_order
    ADD COLUMN coupon_code     VARCHAR(30),
    ADD COLUMN discount_amount NUMERIC(10, 2) NOT NULL DEFAULT 0;

CREATE INDEX idx_customer_order_coupon_code ON customer_order (coupon_code);
