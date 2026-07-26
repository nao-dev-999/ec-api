-- customer_order.idはIDENTITYのサロゲートキーであり、決済代行等の外部システムは知り得ない。
-- 外部連携（決済確定通知の突合等）向けの参照番号として、内部IDとは別にorder_numberを持たせる。
-- DEFAULTはJPA経由の insert では@PrePersistが明示値を積むため使われないが、
-- シードSQL等カラムを指定しない生SQL insert向けのフォールバックとして残す。
ALTER TABLE customer_order
    ADD COLUMN order_number VARCHAR(36) NOT NULL DEFAULT gen_random_uuid()::text;

ALTER TABLE customer_order
    ADD CONSTRAINT uq_customer_order_order_number UNIQUE (order_number);
