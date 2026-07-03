CREATE TABLE customer (
    id          BIGSERIAL PRIMARY KEY,
    email       VARCHAR(255) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    created_at  TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at  TIMESTAMPTZ    NULL,
    created_by  VARCHAR(255),
    updated_by  VARCHAR(255),
    deleted_by  VARCHAR(255),
    version     INT NOT NULL DEFAULT 0
);

-- 開発用初期ユーザー（パスワード: password123）
INSERT INTO customer (email, password)
VALUES
    ('yamada@example.com', '$2a$12$lWIfifxI/nsgpI39NVXXFuCQU9VW.sXYJKoIR58J1aBBYn3nA0Q4u'),
    ('tanaka@example.com',  '$2a$12$lWIfifxI/nsgpI39NVXXFuCQU9VW.sXYJKoIR58J1aBBYn3nA0Q4u');

-- ============================================================================
-- Data for Name: product
-- ============================================================================
INSERT INTO public.product (id, name, description, price, stock, created_at, updated_at, deleted_at, created_by, updated_by, deleted_by, version) VALUES
(1, 'ノートPC', '高性能薄型ノートPC 15インチ', 89800.00, 19, '2026-05-17 16:19:47.95566', '2026-05-17 16:39:30.592239', NULL, NULL, NULL, NULL, 1),
(2, 'ワイヤレスマウス', '静音設計 Bluetooth対応', 3980.00, 98, '2026-05-17 16:19:47.95566', '2026-05-17 16:39:30.597121', NULL, NULL, NULL, NULL, 1),
(3, 'メカニカルキーボード', '青軸 フルサイズ USB-C', 12800.00, 48, '2026-05-17 16:19:47.95566', '2026-05-23 18:25:57.979777', NULL, NULL, NULL, NULL, 1),
(4, '4Kモニター', '27インチ IPS パネル HDR対応', 49800.00, 15, '2026-05-17 16:19:47.95566', '2026-05-17 16:19:47.95566', NULL, NULL, NULL, NULL, 0),
(5, 'USBハブ', '7ポート USB3.0 電源付き', 4500.00, 77, '2026-05-17 16:19:47.95566', '2026-05-23 18:25:57.986433', NULL, NULL, NULL, NULL, 1),
(6, 'ヘッドセット', 'ノイズキャンセリング', 15800.00, 30, '2026-05-17 16:39:18.776084', '2026-05-17 16:39:18.776158', NULL, NULL, NULL, NULL, 0),
(7, 'ヘッドセット', 'ノイズキャンセリング', 15800.00, 30, '2026-05-23 16:49:55.181192', '2026-05-23 16:49:55.181468', NULL, NULL, NULL, NULL, 0);

-- ============================================================================
-- Data for Name: customer_order
-- ============================================================================
INSERT INTO public.customer_order (id, customer_name, status, total_amount, created_at, updated_at, deleted_at, created_by, updated_by, deleted_by, version) VALUES
(1, '山田太郎', 'CONFIRMED', 97760.00, '2026-05-17 16:39:30.488915', '2026-05-25 01:47:35.981987', NULL, NULL, NULL, NULL, 4),
(2, '鈴木一郎', 'PENDING', 39100.00, '2026-05-23 18:25:57.792385', '2026-05-23 18:25:57.792432', NULL, NULL, NULL, NULL, 0);


-- ============================================================================
-- Data for Name: customer_order_detail
-- ============================================================================
INSERT INTO public.customer_order_detail (id, customer_order_id, product_id, quantity, unit_price, subtotal, created_at, updated_at, deleted_at, created_by, updated_by, deleted_by, version) VALUES
(1, 1, 1, 1, 89800.00, 89800.00, '2026-05-17 16:39:30.53668', '2026-05-17 16:39:30.536707', NULL, NULL, NULL, NULL, 0),
(2, 1, 2, 2, 3980.00, 7960.00, '2026-05-17 16:39:30.564459', '2026-05-17 16:39:30.564481', NULL, NULL, NULL, NULL, 0),
(3, 2, 3, 2, 12800.00, 25600.00, '2026-05-23 18:25:57.925193', '2026-05-23 18:25:57.92522', NULL, NULL, NULL, NULL, 0),
(4, 2, 5, 3, 4500.00, 13500.00, '2026-05-23 18:25:57.95671', '2026-05-23 18:25:57.956728', NULL, NULL, NULL, NULL, 0);
