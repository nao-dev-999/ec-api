-- V24__add_image_url_to_product.sql
-- 商品のメイン画像URL(1商品につき1枚)。アップロード基盤は未整備のため、
-- 外部ホスティング済みの画像URLを文字列で保持するのみ。

ALTER TABLE product
    ADD COLUMN image_url VARCHAR(2048);
