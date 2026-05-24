# ECサイトAPI - Spring Boot サンプル

## 技術スタック

| 分類 | 内容 |
|------|------|
| フレームワーク | Spring Boot 4.0.1 |
| ORM | Hibernate 7.1（Spring Data JPA 経由） |
| DB | PostgreSQL |
| 言語 | Java 21 |
| ビルド | Gradle 8.14 (Kotlin DSL) |
| コードフォーマット | Spotless 8.4.0 + Google Java Format 1.18.1 |

## プロジェクト構成

```
ec-api/
├── build.gradle.kts             # ビルド設定・依存関係・Spotless 設定
├── settings.gradle.kts
├── gradle/wrapper/
│   └── gradle-wrapper.properties   # Gradle 8.14
├── src/main/java/com/example/ecapi/
│   ├── EcApiApplication.java
│   ├── config/
│   │   └── DataInitializer.java     # 起動時サンプルデータ投入
│   ├── controller/
│   │   ├── product/
│   │   │   ├── ProductController.java
│   │   │   ├── dto/                 # Web 層専用 DTO (Request/Response)
│   │   │   │   ├── CreateProductRequest.java
│   │   │   │   ├── ProductResponse.java
│   │   │   │   └── UpdateProductRequest.java
│   │   │   └── mapper/              # MapStruct マッパー
│   │   │       └── ProductApiMapper.java
│   │   ├── order/
│   │   │   ├── OrderController.java
│   │   │   ├── dto/                 # Web 層専用 DTO (Request/Response)
│   │   │   │   ├── OrderRequest.java
│   │   │   │   └── OrderResponse.java
│   │   │   └── mapper/              # MapStruct マッパー
│   │   │       └── OrderApiMapper.java
│   ├── service/
│   │   ├── product/
│   │   │   ├── ProductService.java
│   │   │   ├── dto/                 # Service 層専用 DTO (Command/Result)
│   │   │   │   ├── CreateProduct.java
│   │   │   │   ├── ProductResult.java
│   │   │   │   └── UpdateProduct.java
│   │   │   └── mapper/              # MapStruct マッパー
│   │   │       └── ProductEntityMapper.java
│   │   ├── order/
│   │   │   ├── OrderService.java    # 在庫チェック・トランザクション管理
│   │   │   ├── dto/                 # Service 層専用 DTO (Command/Result)
│   │   │   │   ├── CreateOrder.java
│   │   │   │   ├── CreateOrderItem.java
│   │   │   │   ├── OrderResult.java
│   │   │   │   └── OrderResultItem.java
│   │   │   └── mapper/              # MapStruct マッパー
│   │   │       └── OrderEntityMapper.java
│   ├── repository/
│   │   ├── CutomerOrderRepository.java
│   │   ├── ProductRepository.java
│   │   └── ProductSpecification.java # 動的検索用 Specification
│   ├── entity/
│   │   ├── CustomerOrder.java
│   │   ├── CustomerOrderDetail.java
│   │   └── Product.java
│   └── exception/
│       ├── ErrorResponse.java
│       ├── GlobalExceptionHandler.java
│       ├── InsufficientStockException.java
│       ├── OrderNotFoundException.java
│       └── ProductNotFoundException.java
└── src/main/resources/
    ├── application.yml
    └── messages.properties # メッセージ外部化
```

## 起動手順

### 1. docker起動

```bash
docker-compose up -d
```

### 2. 接続設定の確認・変更

`src/main/resources/application.yml` のユーザー名・パスワードを環境に合わせて変更。

### 3. 起動

```bash
./gradlew bootRun
```

起動後、テーブルが自動作成されサンプル商品5件が登録されます。

---

## Spotless の使い方

```bash
# コードを自動整形（Google Java Format）
./gradlew spotlessApply

# フォーマットチェックのみ（CI で使用）
./gradlew spotlessCheck

# ビルドと同時にチェック（build.gradle.kts のコメントを外す）
./gradlew check
```

### Git フック（任意）

プッシュ前に自動チェックするフックを設定できます：

```bash
./gradlew spotlessInstallGitPrePushHook
```

---

## API エンドポイント

### 商品 API

| メソッド | URL | 説明 |
|--------|-----|------|
| GET | /api/products | 全商品取得、または検索条件に合致する商品を取得 |
| GET | /api/products?name=xxx&description=yyy&price=zzz | 商品名、商品説明、価格でAND検索（部分一致、価格以下） |
| GET | /api/products/{id} | 商品詳細 |
| POST | /api/products | 商品登録 |
| PUT | /api/products/{id} | 商品更新（楽観ロックのためリクエストボディに `version` が必要） |
| DELETE | /api/products/{id} | 商品削除 |

### 注文 API

| メソッド | URL | 説明 |
|--------|-----|------|
| GET | /api/orders | 全注文取得 |
| GET | /api/orders/{id} | 注文詳細 |
| POST | /api/orders | 注文作成（在庫チェックあり） |
| PATCH | /api/orders/{id}/status?status=CONFIRMED | ステータス更新（サービス層で楽観ロック適用） |
| PATCH | /api/orders/{id}/cancel | 注文キャンセル（サービス層で楽観ロック適用） |

---

## 動作確認（curl）

```bash
# 一覧
curl http://localhost:8080/api/products | jq
curl http://localhost:8080/api/orders | jq

# ID指定
curl http://localhost:8080/api/products/2 | jq
curl http://localhost:8080/api/orders/1 | jq

# 商品検索 (name, description, price で AND 検索)
curl "http://localhost:8080/api/products?name=$(echo -n 'USBハブ' | jq -sRr @uri)" | jq
curl "http://localhost:8080/api/products?description=$(echo -n '15インチ' | jq -sRr @uri)" | jq
curl "http://localhost:8080/api/products?price=10000" | jq
curl "http://localhost:8080/api/products?name=$(echo -n 'マウス' | jq -sRr @uri)&price=10000" | jq

# 商品登録
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{"name":"ヘッドセット","description":"ノイズキャンセリング","price":15800,"stock":30}' | jq

# 商品更新 (version を含める必要がある)
# まず現在の version を取得
PRODUCT_ID=1
VERSION=$(curl http://localhost:8080/api/products/${PRODUCT_ID} | jq -r .version)
curl -X PUT http://localhost:8080/api/products/${PRODUCT_ID} \
  -H "Content-Type: application/json" \
  -d '{
    "name":"更新された商品名",
    "description":"更新された説明",
    "price":12345,
    "stock":25,
    "version":'${VERSION}'
  }' | jq

# 注文作成（在庫が自動減算される）
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerName": "鈴木一郎",
    "items": [
      {"productId": 3, "quantity": 2},
      {"productId": 5, "quantity": 3}
    ]
  }' | jq

# ステータス更新
curl -X PATCH "http://localhost:8080/api/orders/1/status?status=CONFIRMED" | jq
```

---

## 学習ポイント

1. **Gradle Kotlin DSL** `build.gradle.kts` による型安全なビルド設定
2. **Spotless** Google Java Format でチーム全体のコードスタイルを統一
3. **Hibernate 7** Spring Boot 4.0 に同梱される最新 ORM。Jakarta EE 11 ベース
4. **レイヤードアーキテクチャ** Controller → Service → Repository の役割分担
5. **Spring Data JPA** メソッド名クエリ・`@Query` によるカスタムクエリ・`Specification` による動的検索
6. **トランザクション管理** `@Transactional` で在庫チェック〜注文〜在庫減算を原子的に実行
7. **N+1 問題対策** `LEFT JOIN FETCH` で関連エンティティを一括取得
8. **楽観ロック** `@Version` アノテーションによるデータ競合の防止
9. **例外ハンドリング** `@RestControllerAdvice` とカスタム例外（`ProductNotFoundException`, `OrderNotFoundException`, `InsufficientStockException` など）による統一エラーレスポンス
10. **メッセージの外部化 (i18n)** `messages.properties` と `MessageSource` によるハードコード文字列の排除
