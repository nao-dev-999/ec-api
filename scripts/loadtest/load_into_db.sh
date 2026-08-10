#!/usr/bin/env bash
# generate_load_test_data.py が出力した CSV を PostgreSQL に投入する。
#
# `\copy` (psql のクライアント側コピー)を使うため、RDS のようにサーバー側の
# ファイルシステムにアクセスできない環境でも実行できる。
#
# 使い方:
#   PGHOST=xxx.rds.amazonaws.com PGPORT=5432 PGDATABASE=ec_db PGUSER=postgres PGPASSWORD=xxx \
#     ./scripts/loadtest/load_into_db.sh scripts/loadtest/out
#
# PG* 環境変数は psql/libpq標準のものをそのまま使う。

set -euo pipefail

DATA_DIR="${1:-scripts/loadtest/out}"

if [ ! -d "${DATA_DIR}" ]; then
  echo "データディレクトリが見つかりません: ${DATA_DIR}" >&2
  exit 1
fi

echo "=== 投入開始: ${DATA_DIR} ==="

# FK制約を満たす順序で投入する。列リストは generate_load_test_data.py の CSV ヘッダーと
# 完全に一致させること(順序がずれると位置ベースのマッピングが壊れる)。

psql -v ON_ERROR_STOP=1 <<SQL
\timing on

\echo '--- category ---'
\copy category (id, name, created_at, updated_at, version, is_deleted) FROM '${DATA_DIR}/category.csv' WITH (FORMAT csv, HEADER true)

\echo '--- product ---'
\copy product (id, name, description, price, stock, created_at, updated_at, version, is_deleted, image_url) FROM '${DATA_DIR}/product.csv' WITH (FORMAT csv, HEADER true)

\echo '--- product_category ---'
\copy product_category (product_id, category_id) FROM '${DATA_DIR}/product_category.csv' WITH (FORMAT csv, HEADER true)

\echo '--- customer ---'
\copy customer (id, email, password, last_name, first_name, phone_number, created_at, updated_at, version, is_deleted) FROM '${DATA_DIR}/customer.csv' WITH (FORMAT csv, HEADER true)

\echo '--- customer_order (order_number は DEFAULT で自動生成) ---'
\copy customer_order (id, customer_id, status, ordered_at, total_amount, created_at, updated_at, version, is_deleted, coupon_code, discount_amount, shipping_recipient_name, shipping_postal_code, shipping_prefecture, shipping_city, shipping_address_line1, shipping_phone_number) FROM '${DATA_DIR}/customer_order.csv' WITH (FORMAT csv, HEADER true)

\echo '--- customer_order_detail ---'
\copy customer_order_detail (id, customer_order_id, product_id, quantity, unit_price, subtotal, created_at, updated_at, version) FROM '${DATA_DIR}/customer_order_detail.csv' WITH (FORMAT csv, HEADER true)

\echo '--- review ---'
\copy review (id, customer_id, product_id, rating, comment, created_at, updated_at, version) FROM '${DATA_DIR}/review.csv' WITH (FORMAT csv, HEADER true)

\echo '--- cart_item ---'
\copy cart_item (id, customer_id, product_id, quantity, created_at, updated_at, version) FROM '${DATA_DIR}/cart_item.csv' WITH (FORMAT csv, HEADER true)

-- 明示的にIDを指定してINSERTしたため、後続のアプリ側INSERT(サービス経由)でID衝突しないよう
-- シーケンスを投入済み最大IDまで進める。
\echo '--- sequence の再同期 ---'
SELECT setval(pg_get_serial_sequence('category', 'id'), COALESCE((SELECT MAX(id) FROM category), 1));
SELECT setval(pg_get_serial_sequence('product', 'id'), COALESCE((SELECT MAX(id) FROM product), 1));
SELECT setval(pg_get_serial_sequence('customer', 'id'), COALESCE((SELECT MAX(id) FROM customer), 1));
SELECT setval(pg_get_serial_sequence('customer_order', 'id'), COALESCE((SELECT MAX(id) FROM customer_order), 1));
SELECT setval(pg_get_serial_sequence('customer_order_detail', 'id'), COALESCE((SELECT MAX(id) FROM customer_order_detail), 1));
SELECT setval(pg_get_serial_sequence('review', 'id'), COALESCE((SELECT MAX(id) FROM review), 1));
SELECT setval(pg_get_serial_sequence('cart_item', 'id'), COALESCE((SELECT MAX(id) FROM cart_item), 1));

-- 大量投入直後はプランナー統計が古いままなので、負荷試験前に必ず更新する。
\echo '--- ANALYZE ---'
ANALYZE category;
ANALYZE product;
ANALYZE product_category;
ANALYZE customer;
ANALYZE customer_order;
ANALYZE customer_order_detail;
ANALYZE review;
ANALYZE cart_item;
SQL

echo "=== 投入完了 ==="
