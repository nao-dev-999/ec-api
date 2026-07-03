# 9. データベース / Flyway

> [← インデックスに戻る](../コーディング規約.md)

---

## マイグレーションファイル命名

```
V{version}__{description}.sql
```

- description はスネークケース
- 例: `V1__initial_schema.sql`, `V4__add_employee.sql`

---

## DDL 規約

```sql
-- カラム型
-- 文字列: VARCHAR(n)
-- 主キー: BIGSERIAL または BIGINT GENERATED ALWAYS AS IDENTITY
-- 日時: TIMESTAMPTZ（TIMESTAMPは使わない）
-- 論理削除: is_deleted BOOLEAN NOT NULL DEFAULT FALSE
-- 監査: created_by / updated_by は BIGINT（ユーザID）

CREATE TABLE products (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(255)    NOT NULL,
    price       INTEGER         NOT NULL CHECK (price >= 0),
    stock       INTEGER         NOT NULL CHECK (stock >= 0),
    is_deleted  BOOLEAN         NOT NULL DEFAULT FALSE,
    version     BIGINT          NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    created_by  BIGINT          NOT NULL,
    updated_by  BIGINT          NOT NULL
);
```

**原則:**

- `ddl-auto: validate` を本番・ステージングで使用（エンティティとスキーマの乖離を検出）
- `ddl-auto: create-drop` はローカル開発のみ
- 外部キーには必ずインデックスを貼る
- パスワードハッシュは `customer` テーブルに置かず、`customer_auth` テーブルに分離する
