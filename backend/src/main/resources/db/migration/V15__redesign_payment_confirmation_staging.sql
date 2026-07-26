-- V13の列構成（order_id, amount, settled_at）は、決済代行が知り得ない内部サロゲートキー(order_id)を
-- 突合キーとして前提にしていたため、実運用の受信I/Fとして成立しない設計だった。
-- 決済代行からの決済確定通知として現実的な列に再設計する:
-- ・order_id(内部サロゲートキー) -> order_number（customer_order.order_numberに対応する外部連携用参照番号）
-- ・transaction_id（決済代行側の取引ID）、customer_id（突合用の顧客識別子。氏名等のPIIは持たない）、
--   payment_method（決済手段）、status（決済ステータス）を追加
-- job_instance_id + order_numberをPKとするのはV13と同じ理由（再実行時の二重ステージング防止）。
-- 本番未リリースの作業用テーブルのため、ALTERではなく作り直す。
DROP TABLE payment_confirmation_staging;

CREATE TABLE payment_confirmation_staging (
    job_instance_id BIGINT         NOT NULL,
    order_number    VARCHAR(36)    NOT NULL,
    transaction_id  VARCHAR(64)    NOT NULL,
    customer_id     BIGINT         NOT NULL,
    payment_method  VARCHAR(20)    NOT NULL,
    status          VARCHAR(20)    NOT NULL,
    amount          DECIMAL(19, 2) NOT NULL,
    settled_at      TIMESTAMPTZ    NOT NULL,
    PRIMARY KEY (job_instance_id, order_number)
);
