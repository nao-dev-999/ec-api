-- V25__drop_customer_address_columns.sql
-- 顧客の単一住所を廃止し、複数配送先住所を管理する shipping_address テーブルに一元化する。

ALTER TABLE customer
    DROP COLUMN postal_code,
    DROP COLUMN prefecture,
    DROP COLUMN city,
    DROP COLUMN address_line1,
    DROP COLUMN address_line2;
