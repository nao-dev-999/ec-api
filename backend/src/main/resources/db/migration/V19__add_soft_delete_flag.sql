-- V19__add_soft_delete_flag.sql
-- 論理削除の完成: deleted_at/deleted_by は導入以来どのコードからも参照・更新されない未使用カラムだったため、
-- ドキュメント（backend/docs/04-entity-design.md 4.5節）が定める is_deleted フラグ方式に置き換える。

-- 独立したライフサイクルを持ち、外部から参照される親レコード（product/customer/employee/category/customer_order）にのみ付与する。
-- cart_item・customer_order_detail・payment は論理削除の対象外（前者2つは親のライフサイクルに従って物理削除される想定、
-- payment は削除フロー自体が存在しない）。

ALTER TABLE product
    ADD COLUMN is_deleted BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE customer
    ADD COLUMN is_deleted BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE employee
    ADD COLUMN is_deleted BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE category
    ADD COLUMN is_deleted BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE customer_order
    ADD COLUMN is_deleted BOOLEAN NOT NULL DEFAULT FALSE;

-- 未使用カラムの削除（全8テーブル共通）
ALTER TABLE product
    DROP COLUMN deleted_at,
    DROP COLUMN deleted_by;

ALTER TABLE customer
    DROP COLUMN deleted_at,
    DROP COLUMN deleted_by;

ALTER TABLE employee
    DROP COLUMN deleted_at,
    DROP COLUMN deleted_by;

ALTER TABLE category
    DROP COLUMN deleted_at,
    DROP COLUMN deleted_by;

ALTER TABLE customer_order
    DROP COLUMN deleted_at,
    DROP COLUMN deleted_by;

ALTER TABLE customer_order_detail
    DROP COLUMN deleted_at,
    DROP COLUMN deleted_by;

ALTER TABLE cart_item
    DROP COLUMN deleted_at,
    DROP COLUMN deleted_by;

ALTER TABLE payment
    DROP COLUMN deleted_at,
    DROP COLUMN deleted_by;
