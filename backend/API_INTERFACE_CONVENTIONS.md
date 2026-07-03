# REST API インターフェース設計規約

> **対象:** Spring Boot による REST API 設計全般  
> **目的:** クライアントが扱いやすく、セキュアで一貫性のある API を設計するための規約

---

## 目次

1. [RESTの基本方針と限界](#1-restの基本方針と限界)
2. [エンドポイント設計](#2-エンドポイント設計)
3. [検索処理](#3-検索処理)
4. [更新処理](#4-更新処理)
5. [レスポンス設計](#5-レスポンス設計)
6. [エラーレスポンス](#6-エラーレスポンス)
7. [日時・数値・NULL](#7-日時数値null)
8. [ページネーション](#8-ページネーション)
9. [セキュリティ](#9-セキュリティ)
10. [冪等性](#10-冪等性)

---

## 1. RESTの基本方針と限界

### 基本方針

REST はルールではなく設計思想である。**チームで一貫したルールを定めること**が目的であり、RESTの定義に厳密に従うことが目的ではない。

操作がリソースの CRUD に収まるかどうかで設計を使い分ける。

| 操作の性質 | 設計 | 例 |
|-----------|------|----|
| リソースの CRUD | REST（GET / POST / PUT / PATCH / DELETE） | 商品の取得・作成・更新・削除 |
| ビジネスアクション | `POST /resource/{id}/{action}` | 注文キャンセル・出荷 |
| 複雑な検索 | `POST /resource/search` | 複数条件・OR条件を含む検索 |
| 一括操作 | `POST /resource/bulk-{action}` | 複数商品の一括無効化 |

### GETの構造的な限界

GETには以下の制約があり、用途を誤るとセキュリティ・機能上の問題が発生する。

**① クエリパラメータはログに残る**

WebサーバのアクセスログRoad、ロードバランサのログ、ブラウザ履歴、`Referer` ヘッダーにURLがそのまま記録される。

```
// Nginxアクセスログへの記録例
192.168.1.1 "GET /api/customers?email=user@example.com&name=山田太郎 HTTP/1.1" 200
```

個人情報・センシティブな検索条件をクエリパラメータに含めてはならない。

**② GETはキャッシュされる**

ブラウザ・CDN・プロキシはGETレスポンスをキャッシュする。残高・在庫・通知等のリアルタイム性が必要なリソースでは `Cache-Control: no-store` を必ず付与するか、POSTで取得する。

```
// リアルタイム性が必要なエンドポイントには必須
Cache-Control: no-store
```

**③ URLの長さ制限**

ブラウザは約2000文字、多くのサーバのデフォルトは8KB。多数のIDをフィルタ条件に指定する場合はPOSTのボディで送る。

### RESTが表現しにくい操作

**状態遷移に条件がある場合**

ステータスを直接書き換える設計はバリデーションの置き場が曖昧になる。アクションエンドポイントにするとビジネスルールが明確になる。

```
// ❌ ステータスを直接書き換える
PUT /api/orders/1
{ "status": "CANCELLED" }  // どこでキャンセル可否を検証する？

// ✅ アクションとして表現する
POST /api/orders/1/cancel   // Service でステータス検証して弾く
POST /api/orders/1/ship
POST /api/orders/1/complete
```

**トランザクションをまたぐ操作**

```
// 送金: 送り元の残高を減らして受け取り側を増やす
// どちらのリソースのエンドポイントか判断できない

// → アクションとして切り出す
POST /api/transfers
{ "fromAccountId": 1, "toAccountId": 2, "amount": 5000 }
```

**副作用のある参照**

GETは副作用なしが原則。取得と同時に状態を変えたい場合は操作を分離する。

```
// ❌ GETで副作用を持たせる
GET /api/notifications  // 取得と同時に既読にする

// ✅ 操作を分離する
GET  /api/notifications           // 取得のみ
POST /api/notifications/read-all  // 既読処理
```

**複数リソースにまたがる一括取得**

画面に必要なデータが複数リソースにまたがる場合、RESTに忠実にすると複数リクエストが必要になる。画面専用のエンドポイントを作るか、GraphQLを検討する。

```
// RESTに忠実にすると4リクエスト必要
GET /api/orders/1
GET /api/products/5
GET /api/customers/3
GET /api/shipments/1

// → 画面専用エンドポイント（BFF的発想）
GET /api/orders/1/detail  // 必要なものをまとめて返す
```

---

## 2. エンドポイント設計

### URL設計

```
// リソースは複数形・名詞
/api/products          // 商品一覧
/api/products/{id}     // 商品詳細
/api/orders/{id}/items // 注文明細（親子関係）

// アクションは動詞
/api/orders/{id}/cancel
/api/orders/{id}/ship

// 検索
/api/products/search

// 一括操作
/api/products/bulk-disable
```

**禁止事項:**

```
// ❌ 動詞をリソースURIに含めない
/api/getProducts
/api/createProduct
/api/deleteProduct?id=1

// ❌ 動詞的な名詞も避ける
/api/productList
/api/productDetail
```

### HTTPメソッドと用途

| メソッド | 用途 | 冪等 | ボディ |
|---------|------|------|--------|
| GET | 取得 | ✅ | なし |
| POST | 作成・アクション・検索 | ❌ | あり |
| PUT | リソース全体の置き換え | ✅ | あり |
| PATCH | 部分更新 | ❌ | あり |
| DELETE | 削除 | ✅ | 原則なし |

### HTTPステータスコード

| コード | 用途 |
|--------|------|
| 200 OK | 取得・更新成功 |
| 201 Created | 作成成功 |
| 204 No Content | 削除成功・ボディなし |
| 400 Bad Request | バリデーションエラー・不正なリクエスト |
| 401 Unauthorized | 未認証 |
| 403 Forbidden | 認可エラー（認証済みだが権限なし） |
| 404 Not Found | リソースが存在しない |
| 409 Conflict | 楽観ロック競合・重複登録 |
| 422 Unprocessable Entity | 形式は正しいがビジネスルール違反 |
| 500 Internal Server Error | サーバ内部エラー |

400と422の使い分け:
- 400: 型が違う・必須項目なし等、形式上のエラー
- 422: 形式は正しいが「在庫がない」「キャンセル済みの注文はキャンセルできない」等のビジネスルール違反

---

## 3. 検索処理

### GETクエリパラメータを使う条件

条件がシンプルで、センシティブな情報を含まない場合に使用する。

```
GET /api/products?name=商品A&minPrice=100&maxPrice=5000&sort=price,asc&page=0&size=20
```

使用基準:
- 条件が5個以下
- OR条件・ネストした条件がない
- 個人情報・センシティブな値を含まない
- ブックマーク・URL共有を許容するエンドポイント

### POST /search を使う条件

```
POST /api/products/search
{
  "name": "商品A",
  "priceRange": { "min": 100, "max": 5000 },
  "categories": ["food", "drink"],
  "inStock": true,
  "sort": { "field": "price", "direction": "asc" },
  "page": { "number": 0, "size": 20 }
}
```

使用基準:
- 条件が多い（目安6個以上）
- OR条件・配列条件・ネストした条件がある
- メールアドレス・氏名・住所等の個人情報を含む
- 多数のIDをフィルタ条件に指定する

### センシティブな検索は必ずPOST

```
// ❌ 個人情報がURLに露出・ログに残る
GET /api/customers?email=user@example.com&name=山田太郎

// ✅ ボディで送る
POST /api/customers/search
{ "email": "user@example.com", "name": "山田太郎" }
```

### ソート

```
// クエリパラメータの場合
?sort=createdAt,desc&sort=name,asc   // 複数ソートキー

// POST /search ボディの場合
"sort": [
  { "field": "createdAt", "direction": "desc" },
  { "field": "name",      "direction": "asc" }
]
```

ソートキーはクライアントが指定する文字列をそのままSQLに渡さない。許可するフィールドをホワイトリストで管理する。

```java
// ✅ ホワイトリストで検証
private static final Set<String> SORTABLE_FIELDS =
    Set.of("name", "price", "createdAt", "updatedAt");

if (!SORTABLE_FIELDS.contains(sortField)) {
    throw new InvalidSortFieldException(sortField);
}
```

---

## 4. 更新処理

### PUT（全体置き換え）

リソース全体を送信する。送らなかったフィールドは `null` になる。全項目編集フォームに向いている。

```
PUT /api/products/1
{
  "name": "商品A改",
  "price": 1200,
  "stock": 10       // 全フィールド必須
}
```

### PATCH（部分更新）

送信したフィールドだけを更新する。「メールアドレスだけ変更」等の部分更新に向いている。

```
PATCH /api/customers/1
{ "email": "new@example.com" }   // emailだけ更新
```

**PATCHの実装上の注意 — null と未送信の区別:**

```json
// これはemailをnullにしたいのか、フィールド自体を送っていないのか区別できない
{ "name": "山田太郎" }
```

Javaでの対処:

```java
// パターンA: Optional で包む
public record PatchCustomerRequest(
    Optional<String> name,    // null = 未送信、Optional.of(value) = 更新あり
    Optional<String> email
) {}

// Service での適用
if (request.name() != null) {
    customer.setName(request.name().orElse(null));
}
```

**PUT/PATCHの使い分け指針:**

管理画面等の全項目編集フォームは PUT に統一するとシンプルになる。顧客向けの部分更新（パスワード変更・プロフィール更新等）は PATCH を使う。迷う場合は PUT に統一する。

### 楽観ロックとの組み合わせ

PUT・PATCHのリクエストには `version` を含める。削除リクエストも同様。

```json
// PUT リクエスト
{
  "name": "商品A改",
  "price": 1200,
  "stock": 10,
  "version": 3      // 取得時の version をそのまま送る
}

// DELETE リクエスト（クエリパラメータで送る）
DELETE /api/products/1?version=3
```

楽観ロックを「効かせない」操作（管理者の強制上書き・バッチ処理）では `version` を受け取らない専用エンドポイントを用意するか、ヘッダーで区別する。

### 状態遷移はアクションエンドポイント

ステータスの直接書き換えは避け、意味のあるアクションとして表現する。

```
// ❌ ステータスを直接書き換える
PUT /api/orders/1 { "status": "CANCELLED" }

// ✅ アクションエンドポイント
POST /api/orders/1/cancel
POST /api/orders/1/ship
POST /api/orders/1/complete
```

アクションエンドポイントのリクエストボディには理由・備考等の付随情報を持たせる。

```json
POST /api/orders/1/cancel
{
  "reason": "CUSTOMER_REQUEST",
  "note": "顧客より連絡あり"
}
```

---

## 5. レスポンス設計

### レスポンス構造

素のオブジェクトを返す（エンベロープで包まない）。

```json
// ✅ シンプルな単一リソース
{
  "id": 1,
  "name": "商品A",
  "price": 1000,
  "stock": 10,
  "version": 3,
  "createdAt": "2026-06-27T10:00:00Z",
  "updatedAt": "2026-06-27T12:00:00Z"
}

// ❌ 不要なエンベロープ
{
  "data": { "id": 1, ... },
  "meta": { "timestamp": "..." }
}
```

エンベロープはGraphQLやモバイルファースト設計で採用されるが、REST APIでは一般的でない。

### レスポンスに含めてはならない情報

以下はレスポンスに**絶対に含めない**。エンティティを直接返さず専用のレスポンスDTOを用意することで防ぐ。

- パスワードハッシュ
- セッショントークン・認証トークン
- 内部実装の詳細（スタックトレース・SQLエラーメッセージ等）
- 他ユーザの個人情報

```java
// ❌ エンティティをそのまま返す（passwordHashが露出するリスク）
return customerRepository.findById(id);

// ✅ レスポンスDTOに変換して返す
return mapper.toCustomerResponse(customer);  // passwordHashはDTOに含めない
```

### フィールドの命名

JSONのキーは `camelCase` に統一する。

```json
// ✅
{ "createdAt": "2026-06-27T10:00:00Z", "totalAmount": 5000 }

// ❌
{ "created_at": "2026-06-27T10:00:00Z", "total_amount": 5000 }
```

JacksonのデフォルトはcamelCaseのため特別な設定は不要。

---

## 6. エラーレスポンス

### 統一フォーマット

全エラーレスポンスを同一フォーマットで返す。クライアントがエラー種別をプログラムで判定できる `code` を必ず含める。

```json
// 単一エラー
{
  "code": "PRODUCT_NOT_FOUND",
  "message": "商品が見つかりません（id=99）",
  "timestamp": "2026-06-27T10:00:00Z",
  "path": "/api/products/99"
}

// バリデーションエラー（複数フィールド）
{
  "code": "VALIDATION_ERROR",
  "message": "入力値が不正です",
  "timestamp": "2026-06-27T10:00:00Z",
  "path": "/api/products",
  "errors": [
    { "field": "price", "message": "0以上の値を入力してください" },
    { "field": "name",  "message": "必須項目です" }
  ]
}
```

### エラーコード命名規則

```
{RESOURCE}_{REASON}

PRODUCT_NOT_FOUND
ORDER_NOT_FOUND
INSUFFICIENT_STOCK
CONFLICT              // 楽観ロック競合
VALIDATION_ERROR
UNAUTHORIZED
FORBIDDEN
INTERNAL_SERVER_ERROR
```

### 本番環境での注意

本番環境では内部情報をエラーレスポンスに含めない。

```json
// ❌ スタックトレース・SQL情報が露出する
{
  "message": "could not execute statement; SQL [n/a]; constraint [products_name_unique]..."
}

// ✅ 汎用メッセージを返し、詳細はサーバログに記録する
{
  "code": "INTERNAL_SERVER_ERROR",
  "message": "予期しないエラーが発生しました"
}
```

---

## 7. 日時・数値・NULL

### 日時フォーマット

全日時は **ISO 8601 / UTC** で統一する。

```json
// ✅
"createdAt": "2026-06-27T10:00:00Z"

// ❌ タイムゾーンなし・独自フォーマット
"createdAt": "2026-06-27 10:00:00"
"createdAt": 1751019600
```

Jacksonのデフォルトはエポックミリ秒のため、以下の設定が必要。

```yaml
# application.yml
spring:
  jackson:
    serialization:
      write-dates-as-timestamps: false
    time-zone: UTC
```

タイムゾーン変換はクライアント側で行う。APIは常にUTCで返す。

### IDのシリアライズ

JavaScriptの `Number.MAX_SAFE_INTEGER`（2^53-1 = 9,007,199,254,740,991）を超えるIDは精度が失われる。フロントエンドがJavaScriptの場合はIDをStringで返すことを検討する。

```json
// ❌ JavaScriptで精度損失が起きる可能性
{ "id": 9007199254740993 }

// ✅ Stringで返す
{ "id": "9007199254740993" }
```

```java
// Jackson でLongをStringにシリアライズ
@JsonSerialize(using = ToStringSerializer.class)
private Long id;
```

### NULLの扱い

nullフィールドをレスポンスに含めるかどうかをプロジェクト全体で統一する。**原則としてnullフィールドは除外する**。

```java
// application全体に適用
@JsonInclude(JsonInclude.Include.NON_NULL)
```

```json
// NON_NULL あり（フィールドが存在しない）
{ "id": 1, "name": "商品A" }

// NON_NULL なし（nullが露出する）
{ "id": 1, "name": "商品A", "description": null }
```

ただし、「フィールドが存在しない」と「値がnull」を区別する必要がある場合は `NON_NULL` を外してnullを明示する。

---

## 8. ページネーション

一覧取得エンドポイントには必ずページネーションを実装する。ページネーションなしの全件取得エンドポイントは原則作らない。

### リクエスト

```
GET /api/products?page=0&size=20&sort=createdAt,desc
```

| パラメータ | 説明 | デフォルト |
|-----------|------|----------|
| `page` | ページ番号（0始まり） | 0 |
| `size` | 1ページあたりの件数 | 20 |
| `sort` | ソートキー,方向 | createdAt,desc |

`size` の上限を設ける（例: 最大100件）。上限を超えた場合は400を返す。

### レスポンス

```json
{
  "content": [
    { "id": 1, "name": "商品A", "price": 1000 },
    { "id": 2, "name": "商品B", "price": 2000 }
  ],
  "page": {
    "number": 0,
    "size": 20,
    "totalElements": 150,
    "totalPages": 8
  }
}
```

Spring Data の `Page<T>` を使うとこの構造に近いレスポンスを自動生成できる。

### カーソルベースページネーション

件数が多い・リアルタイム更新がある場合はオフセットベース（`page=N`）では取得漏れ・重複が発生する。その場合はカーソルベースを検討する。

```json
// カーソルベース
GET /api/notifications?cursor=eyJpZCI6MTAwfQ&size=20

// レスポンス
{
  "content": [ ... ],
  "nextCursor": "eyJpZCI6ODB9",   // 次ページのカーソル
  "hasNext": true
}
```

---

## 9. セキュリティ

### 認証情報はURLに含めない

```
// ❌ ログ・履歴に残る
GET /api/data?token=eyJhbGciOiJIUzI1NiJ9...
GET /api/data?apiKey=secret123

// ✅ ヘッダーで送る
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
X-API-Key: secret123
```

### センシティブな操作はGETにしない

```
// ❌ パスワードリセットトークンがURLに露出
GET /api/auth/reset-password?token=abc123&newPassword=secret

// ✅ ボディで送る
POST /api/auth/reset-password
{ "token": "abc123", "newPassword": "secret" }
```

### レート制限

認証エンドポイントには必ずレート制限を設ける。総当たり攻撃の対策。

```
POST /api/auth/login  → IPごとに1分間5回まで
POST /api/auth/reset-password → IPごとに1時間3回まで
```

### 他ユーザのリソースへのアクセス制御

URLにIDが含まれる場合、認証済みであっても他ユーザのリソースにアクセスできないことを確認する。

```java
// ❌ IDだけでアクセスできる（他ユーザの注文が見える）
public OrderResult getOrder(Long orderId) {
    return orderRepository.findById(orderId)...
}

// ✅ ログインユーザのIDも条件に加える
public OrderResult getOrder(Long orderId, Long customerId) {
    return orderRepository.findByIdAndCustomerId(orderId, customerId)
        .orElseThrow(() -> new OrderNotFoundException(...));
}
```

---

## 10. 冪等性

### 冪等なメソッドと冪等でないメソッド

| メソッド | 冪等 | 説明 |
|---------|------|------|
| GET | ✅ | 何度呼んでも同じ結果 |
| PUT | ✅ | 同じリクエストを繰り返しても結果は変わらない |
| DELETE | ✅ | 2回目以降は404を返せばよい |
| POST | ❌ | 呼ぶたびにリソースが作られる |
| PATCH | ❌ | 操作によっては結果が変わる |

### POSTの重複送信対策

注文作成・決済等の重要操作では `Idempotency-Key` ヘッダーで重複送信を防ぐ。

```
POST /api/orders
Idempotency-Key: 550e8400-e29b-41d4-a716-446655440000  // クライアントが生成するUUID

// 同じキーで再送した場合: 新規作成せず最初のレスポンスを返す
```

サーバ側の処理:
1. キーをRedis等に保存（TTL付き）
2. 同じキーのリクエストが来たら保存済みレスポンスを返す
3. キーが存在しなければ通常処理を実行してレスポンスを保存

重要度の高い操作（注文・決済・送金）には必ず実装する。

---

## 付録: 設計判断フローチャート

```
操作を設計するとき
        │
        ├─ センシティブな情報を含む検索か？
        │   YES → POST /resource/search
        │   NO  ↓
        │
        ├─ 条件が複雑（6個以上・OR条件・配列）か？
        │   YES → POST /resource/search
        │   NO  → GET + クエリパラメータ
        │
        ├─ リソースのCRUDに収まるか？
        │   YES → GET / POST / PUT / PATCH / DELETE
        │   NO  ↓
        │
        ├─ 状態遷移・ビジネスアクションか？
        │   YES → POST /resource/{id}/{action}
        │
        ├─ 一括操作か？
        │   YES → POST /resource/bulk-{action}
        │
        └─ 複数リソースにまたがる取得か？
            YES → GET /resource/{id}/detail（または GraphQL 検討）
```

---

## 付録: チェックリスト

### エンドポイント設計

- [ ] URLにリソース名が複数形・名詞で含まれている
- [ ] URLに動詞を含めていない（アクションエンドポイントを除く）
- [ ] HTTPメソッドと用途が一致している
- [ ] HTTPステータスコードが適切（201/204/409等を正しく使い分けている）

### 検索

- [ ] センシティブな検索条件をクエリパラメータに含めていない
- [ ] ソートキーのホワイトリスト検証がある
- [ ] 一覧取得にページネーションがある
- [ ] ページサイズに上限がある

### 更新

- [ ] 状態遷移をアクションエンドポイントで表現している
- [ ] PUT/PATCHのリクエストに `version` を含めている
- [ ] PATCHでnullと未送信を正しく区別している

### レスポンス

- [ ] パスワードハッシュ・トークン等がレスポンスに含まれていない
- [ ] エンティティを直接返していない（レスポンスDTOを経由している）
- [ ] 日時がISO 8601 / UTC形式になっている
- [ ] nullフィールドの扱いがプロジェクト全体で統一されている

### セキュリティ

- [ ] 認証情報をURLに含めていない
- [ ] 他ユーザのリソースにアクセスできないよう所有者チェックがある
- [ ] 認証エンドポイントにレート制限がある
- [ ] 重要操作（注文・決済）に冪等性対策がある
