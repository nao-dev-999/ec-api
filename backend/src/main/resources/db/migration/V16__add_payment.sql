-- 決済突合ユースケースの一環。オーソリ〜決済確定の2段階モデルを導入する。
-- customer_orderのstatus（発送等の履行ステータス）とは別軸で決済ステータスを持たせる。
-- 既存行はオーソリ通過済み（決済失敗時はロールバックされレコードが残らない）という前提でAUTHORIZEDを充てる。
ALTER TABLE customer_order
    ADD COLUMN payment_status VARCHAR(20) NOT NULL DEFAULT 'AUTHORIZED';

-- 決済の正マスタ。売上集計テーブルはここから再計算可能な二次データとする。
-- customer_order_idにUNIQUEを置き、現状は1オーダー1決済を前提とする（分割・複数回決済はスコープ外）。
-- transaction_idは決済代行側の取引IDで、payment_confirmation_staging.transaction_idと対応する突合キー。
CREATE TABLE payment (
    id                BIGSERIAL PRIMARY KEY,
    customer_order_id BIGINT         NOT NULL REFERENCES customer_order(id),
    transaction_id    VARCHAR(64)    NOT NULL,
    status            VARCHAR(20)    NOT NULL,
    amount            DECIMAL(19, 2) NOT NULL,
    fee               DECIMAL(19, 2) NOT NULL,
    net_amount        DECIMAL(19, 2) NOT NULL,
    authorized_at     TIMESTAMPTZ    NOT NULL,
    captured_at       TIMESTAMPTZ,
    deleted_at        TIMESTAMPTZ,
    deleted_by        VARCHAR(255),
    version           INTEGER        NOT NULL DEFAULT 0,
    created_at        TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    created_by        BIGINT,
    updated_by        BIGINT,
    UNIQUE (customer_order_id)
);

CREATE INDEX idx_payment_status_captured_at ON payment (status, captured_at);
