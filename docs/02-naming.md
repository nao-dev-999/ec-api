# 2. 命名規則

> [← インデックスに戻る](../コーディング規約.md)

---

## クラス名

| 種別 | 規則 | 例 |
|------|------|----|
| Controller | `{Resource}Controller` | `ProductController`, `AdminOrderController` |
| Service | `{Resource}Service` | `ProductService`, `OrderService` |
| Repository | `{Entity}Repository` | `ProductRepository`, `CustomerOrderRepository` |
| Entity | 単数形・名詞 | `Product`, `CustomerOrder`, `CustomerOrderDetail` |
| Controller DTO (Request) | `{Action}{Resource}Request` | `CreateProductRequest`, `LoginRequest` |
| Controller DTO (Response) | `{Resource}Response` | `ProductResponse`, `OrderResponse` |
| Service DTO (Input) | `{Action}{Resource}` | `CreateProduct`, `UpdateProduct` |
| Service DTO (Output) | `{Resource}Result` | `ProductResult`, `OrderResult` |
| Mapper (Controller層) | `{Resource}ApiMapper` | `ProductApiMapper`, `OrderApiMapper` |
| Mapper (Service層) | `{Resource}EntityMapper` | `ProductEntityMapper`, `OrderEntityMapper` |
| 例外クラス | `{Resource}NotFoundException`, `{Reason}Exception` | `ProductNotFoundException`, `InsufficientStockException` |

## メソッド名

| 操作 | 規則 | 例 |
|------|------|----|
| 単一取得 | `get{Resource}` | `getProduct`, `getOrder` |
| 一覧取得 | `list{Resource}s` | `listProducts`, `listOrders` |
| 作成 | `create{Resource}` | `createProduct` |
| 更新 | `update{Resource}` | `updateProduct` |
| 削除 | `delete{Resource}` | `deleteProduct` |
| キャンセル等の状態遷移 | 動詞 | `cancel`, `ship`, `complete` |

## フィールド名

- `camelCase` を使用する
- `boolean` フィールドは `is` プレフィックスを付けない（JPA/Jackson の相互作用を避けるため）
  - ❌ `isDeleted` → ✅ `deleted`
  - ただし DB カラム名は `is_deleted` を許容する（`@Column(name = "is_deleted")`）
