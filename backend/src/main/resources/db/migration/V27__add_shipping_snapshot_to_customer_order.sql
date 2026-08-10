-- V27__add_shipping_snapshot_to_customer_order.sql
-- 注文確定時点の配送先住所のスナップショットを保持する。
-- 既存注文には値が無いため各カラムはNULL許容とする（新規注文では必須項目として扱う）。

ALTER TABLE customer_order
    ADD COLUMN shipping_recipient_name VARCHAR(100),
    ADD COLUMN shipping_postal_code    VARCHAR(10),
    ADD COLUMN shipping_prefecture     VARCHAR(255),
    ADD COLUMN shipping_city           VARCHAR(255),
    ADD COLUMN shipping_address_line1  VARCHAR(255),
    ADD COLUMN shipping_address_line2  VARCHAR(255),
    ADD COLUMN shipping_phone_number   VARCHAR(20);
