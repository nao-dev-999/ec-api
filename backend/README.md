# ECサイトAPI - Spring Boot サンプル
![CI](https://github.com/kazuya2099/ec-api/actions/workflows/ci.yml/badge.svg)

管理画面向け（admin）と購入者向け（customer）の2系統のAPIを提供するECサイトバックエンド。

## 技術スタック

| 分類 | 内容                                         |
|------|--------------------------------------------|
| フレームワーク | Spring Boot 4.0.1 (Spring WebMVC)          |
| ORM | Hibernate 7.1（Spring Data JPA 経由）          |
| DB | PostgreSQL                                 |
| マイグレーション | Flyway                                     |
| 言語 | Java 25                                    |
| ビルド | Gradle 8.14 (Kotlin DSL)                   |
| コードフォーマット | Spotless 8.4.0 + Google Java Format 1.18.1 |
| カバレッジ計測 | JaCoCo                                     |
| 認証・セッション管理 | Spring Security + Spring Session Redis     |

## プロジェクト構成

パッケージ構成・命名規則・レイヤーごとの責務など詳細な設計方針は [`docs/`](docs/) 配下にまとまっている。特に構成の全体像は [`docs/01-package-structure.md`](docs/01-package-structure.md) を参照。

| ドキュメント | 内容 |
|---|---|
| [01-package-structure.md](docs/01-package-structure.md) | パッケージ構成 |
| [02-naming.md](docs/02-naming.md) | 命名規則 |
| [03-layer-responsibilities.md](docs/03-layer-responsibilities.md) | レイヤー責務 |
| [04-entity-design.md](docs/04-entity-design.md) | エンティティ設計 |
| [05-dto-design.md](docs/05-dto-design.md) | DTO設計 |
| [06-exception-handling.md](docs/06-exception-handling.md) | 例外処理 |
| [07-spring-security.md](docs/07-spring-security.md) | 認証・認可 |
| [08-testing.md](docs/08-testing.md) | テスト方針 |
| [09-database-flyway.md](docs/09-database-flyway.md) | DBマイグレーション |
| [10-formatting.md](docs/10-formatting.md) | フォーマット |
| [11-async-virtual-threads.md](docs/11-async-virtual-threads.md) | 非同期・仮想スレッド |
| [12-logging.md](docs/12-logging.md) | ロギング |
| [13-filter.md](docs/13-filter.md) | フィルター |
| [appendix-checklist.md](docs/appendix-checklist.md) | チェックリスト |

## CI/CD

```
GitHub push
  │
  ├─► GitHub Actions CI (backend/.github/workflows/ci.yml)
  │     └─ spotlessCheck → test（PostgreSQL/Redisサービスコンテナ使用）
  │
  └─► main へのマージ後: AWS CodePipeline（Terraform管理: infrastructure/terraform）
        ├─ Source: GitHub (CodeStar Connections)
        ├─ Build: CodeBuild → Docker build（アプリ + Flyway） → ECR push
        ├─ Migrate: ECS Run Task（Flywayコンテナでマイグレーション実行、失敗時はデプロイ中断）
        └─ Deploy: ECS Fargate サービスのローリングアップデート
```
## terraform実行手順
### ステップ1: 土台（ネットワークとリポ）を最優先で作成
terraform apply -target=module.vpc -target=module.ecr

### ステップ2: 永続レイア（DB）の作成
terraform apply -target=module.rds

### ステップ3: 残り（ALB, ECS, CodePipeline）をすべて作成
terraform apply


## 起動手順

### 1. 環境変数ファイルの準備

```bash
cp .env.example .env
# 必要に応じて POSTGRES_PASSWORD などを変更
```

### 2. DB・Redisの起動

```bash
docker-compose up -d
```

### 3. アプリ起動（localプロファイル）

```bash
SPRING_PROFILES_ACTIVE=local SPRING_DATASOURCE_PASSWORD=changeme ./gradlew bootRun
```

起動時にFlywayが `src/main/resources/db/migration` のマイグレーションを自動適用してスキーマを構築する（`spring.jpa.hibernate.ddl-auto=validate` のためJPAはテーブルを作成しない）。`local`プロファイルでは併せて `src/main/resources/db/seed/V9__init_data.sql` が適用され、商品100件・顧客50件・従業員50件（いずれもパスワードは `password123`）が投入される。

---

### Git フック（任意）

プッシュ前に自動チェックするフックを設定できます：

```bash
./gradlew spotlessInstallGitPrePushHook
```

---

## API エンドポイント

認証はロールベースのセッション認証（Spring Security + Spring Session Redis）。管理者向け（`ADMIN`/`PRODUCT_MANAGER`/`SALES`）は `/api/admin/**`・`/api/auth/**`、購入者向け（`CUSTOMER`）は `/api/customer/**`・`/api/orders/**` を利用する（詳細は [docs/07-spring-security.md](docs/07-spring-security.md)）。全エンドポイントの詳細な仕様はSwagger UIを参照。

### 管理者向け（admin）

| 機能 | ベースURL | 説明 |
|---|---|---|
| 認証 | `/api/auth` | ログイン / ログアウト |
| 商品 | `/api/admin/products` | 商品CRUD、`/{productId}/categories` でカテゴリ紐付け |
| カテゴリ | `/api/admin/categories` | カテゴリCRUD |
| 従業員 | `/api/admin/employees` | 従業員管理、ロール変更 |
| 顧客 | `/api/admin/customers` | 顧客参照・削除 |
| 注文 | `/api/admin/orders` | 全顧客の注文参照、ステータス更新 |

### 購入者向け（customer）

| 機能 | ベースURL | 説明 |
|---|---|---|
| 認証 | `/api/customer/auth` | ログイン / ログアウト |
| 商品 | `/api/customer/products` | 商品一覧・検索・詳細（認証不要） |
| カート | `/api/customer/cart` | カート参照・追加・数量更新・削除 |
| マイページ | `/api/customer/me` | 自分の情報参照、メール/パスワード変更 |
| 注文 | `/api/orders` | 注文一覧・詳細・作成・ステータス更新 |

---
## swagger
http://localhost:8080/swagger-ui/index.html （`local`プロファイルでは `/swagger-ui.html`）

## 動作確認（curl）

```bash
### 管理者ログイン (セッションクッキーを取得)
curl -v -c admin_cookies.txt -H "Content-Type: application/json" \
  -d '{"email":"satou@example.com","password":"password"}' \
  http://localhost:8080/api/auth/login

### 商品登録 (管理者、セッションクッキーを使用)
curl -s -b admin_cookies.txt -X POST http://localhost:8080/api/admin/products \
  -H "Content-Type: application/json" \
  -d '{"name":"ヘッドセット","description":"ノイズキャンセリング","price":15800,"stock":30}' | jq

# 商品更新 (楽観ロックのため version を含める必要がある)
PRODUCT_ID=1
VERSION=$(curl -s -b admin_cookies.txt http://localhost:8080/api/admin/products/${PRODUCT_ID} | jq -r .version)
curl -s -b admin_cookies.txt -X PUT http://localhost:8080/api/admin/products/${PRODUCT_ID} \
  -H "Content-Type: application/json" \
  -d '{
    "name":"更新された商品名",
    "description":"更新された説明",
    "price":12345,
    "stock":25,
    "version":'${VERSION}'
  }' | jq

### 購入者ログイン (セッションクッキーを取得)
curl -v -c customer_cookies.txt -H "Content-Type: application/json" \
  -d '{"email":"yamada@example.com","password":"password123"}' \
  http://localhost:8080/api/customer/auth/login

# 商品一覧・検索 (認証不要, name/description/price でAND検索)
curl -s http://localhost:8080/api/customer/products | jq
curl -s "http://localhost:8080/api/customer/products?name=$(echo -n 'ワイヤレスマウス' | jq -sRr @uri)" | jq

# カートに追加 (購入者、セッションクッキーを使用)
curl -s -b customer_cookies.txt -X POST http://localhost:8080/api/customer/cart/items \
  -H "Content-Type: application/json" \
  -d '{"productId":2,"quantity":2}' | jq

# カート参照
curl -s -b customer_cookies.txt http://localhost:8080/api/customer/cart | jq

# 注文作成（在庫が自動減算される, 購入者、セッションクッキーを使用）
curl -s -b customer_cookies.txt -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "items": [
      {"productId": 3, "quantity": 2},
      {"productId": 5, "quantity": 3}
    ]
  }' | jq

# 注文ステータス更新 (楽観ロックはサービス層で適用)
curl -s -b customer_cookies.txt -X PATCH "http://localhost:8080/api/orders/1/status?status=CONFIRMED" | jq
```
