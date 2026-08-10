-- 商品名・説明の部分一致検索（LIKE '%keyword%'）を高速化する
-- ProductSpecification は lower(name) LIKE ? / lower(description) LIKE ? を発行するため、
-- 同じ式に対する trigram GIN インデックスを張る。
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX idx_product_name_trgm ON product USING gin (lower(name) gin_trgm_ops);
CREATE INDEX idx_product_description_trgm ON product USING gin (lower(description) gin_trgm_ops);

-- 顧客ごとの注文一覧取得（GET /api/orders）を高速化する
CREATE INDEX idx_customer_order_customer_id ON customer_order (customer_id);
