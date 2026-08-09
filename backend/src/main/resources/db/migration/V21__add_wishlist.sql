-- V21__add_wishlist.sql
-- お気に入り（ウィッシュリスト）機能。1顧客につき同一商品は1件まで（customer_id, product_id のUNIQUE制約）。
-- review・cart_item と同様、親（customer/product）のライフサイクルに従属する軽量な関連のため
-- is_deleted は付与せず、削除は物理削除とする。

CREATE TABLE wishlist_item (
    id          BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL REFERENCES customer(id) ON DELETE CASCADE,
    product_id  BIGINT NOT NULL REFERENCES product(id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by  BIGINT,
    updated_by  BIGINT,
    version     INT NOT NULL DEFAULT 0,
    UNIQUE (customer_id, product_id)
);

CREATE INDEX idx_wishlist_item_customer_id ON wishlist_item (customer_id);
