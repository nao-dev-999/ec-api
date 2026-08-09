-- V20__add_review.sql
-- 商品レビュー機能。1顧客につき1商品1件まで投稿可能（customer_id, product_id のUNIQUE制約）。
-- cart_item・customer_order_detail と同様、親（customer/product）のライフサイクルに従属するため
-- is_deleted は付与せず、削除（顧客本人 or 管理者によるモデレーション）は物理削除とする。

CREATE TABLE review (
    id          BIGSERIAL PRIMARY KEY,
    customer_id BIGINT   NOT NULL REFERENCES customer(id) ON DELETE CASCADE,
    product_id  BIGINT   NOT NULL REFERENCES product(id) ON DELETE CASCADE,
    rating      SMALLINT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment     VARCHAR(1000),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by  BIGINT,
    updated_by  BIGINT,
    version     INT NOT NULL DEFAULT 0,
    UNIQUE (customer_id, product_id)
);

CREATE INDEX idx_review_product_id ON review (product_id);
