CREATE TABLE customer (
    id          BIGSERIAL PRIMARY KEY,
    email       VARCHAR(255) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
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
