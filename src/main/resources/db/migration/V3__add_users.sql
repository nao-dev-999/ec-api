CREATE TABLE customer (
    id          BIGSERIAL PRIMARY KEY,
    email       VARCHAR(255) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    role        VARCHAR(50)  NOT NULL DEFAULT 'ROLE_USER',
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by  VARCHAR(255),
    updated_by  VARCHAR(255),
    deleted_by  VARCHAR(255),
    version     INT NOT NULL DEFAULT 0
);

-- 開発用初期ユーザー（パスワード: password123）
INSERT INTO customer (email, password, role)
VALUES
    ('admin@example.com', '$2b$12$xMFzEoRpvlUCwGoGJBfXf.LJ/1FJiGYSfZf7YXH6Fhb7J7v6KX4Oy', 'ROLE_ADMIN'),
    ('user@example.com',  '$2b$12$xMFzEoRpvlUCwGoGJBfXf.LJ/1FJiGYSfZf7YXH6Fhb7J7v6KX4Oy', 'ROLE_USER');
