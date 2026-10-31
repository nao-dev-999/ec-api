# 21. OpenAPI/Swaggerドキュメント化規約

> [← インデックスに戻る](../コーディング規約.md)

---

`springdoc-openapi` は導入済みで `OpenApiConfig` によりSwagger UIは有効だが、各Controllerに `@Operation`/`@ApiResponse`/`@Schema` 等のアノテーションが付与されておらず、自動生成に完全に依存している。フィールド名・型以上の情報（「なぜこのエラーが返るか」「このフィールドの意味」）がドキュメントに残らず、フロントエンド実装者や将来の外部パートナーが読んでも仕様の意図が分からない状態。

## 方針

すべての新規エンドポイントに以下の最低限のアノテーションを付与する。既存エンドポイントは変更時に順次追加する（一括での書き戻しは行わない）。

```java
@Operation(
    summary = "商品を作成する",
    description = "管理者権限が必要。在庫数は0以上の整数のみ許可する。")
@ApiResponses({
    @ApiResponse(responseCode = "201", description = "作成成功"),
    @ApiResponse(responseCode = "400", description = "バリデーションエラー"),
    @ApiResponse(responseCode = "409", description = "同名商品が既に存在する(PRODUCT_NAME_DUPLICATE)")
})
@PostMapping
public ResponseEntity<AdminProductResponse> createProduct(
        @Valid @RequestBody CreateProductRequest request) { ... }
```

```java
// Request/Response DTOのフィールドにも意味が自明でないものには@Schemaを付与する
public record CreateProductRequest(
    @NotBlank @Size(max = 255) String name,

    @Schema(description = "税抜価格(円)。0以上の整数のみ許可")
    @NotNull @Min(0) Integer price,

    Integer stock
) {}
```

## 付与する情報の基準

| 情報 | 必須/任意 | 理由 |
|---|---|---|
| `summary`（エンドポイントの概要） | 必須 | 一覧画面(Swagger UI)でエンドポイントの目的が一目でわかる |
| `description`（権限・制約の補足） | 制約がある場合必須 | 「管理者限定」「冪等性あり」等、型だけでは表現できない情報を残す |
| 想定されるエラーレスポンス(`@ApiResponse`) | 409/404等、ビジネス例外を返しうる場合は必須 | クライアント実装者がどの `code` をハンドリングすべきか事前にわかる |
| フィールドの単位・制約の意図(`@Schema(description=...)`) | 単位が自明でない場合必須（金額・期間等） | 「価格は税込か税抜か」のような誤解を防ぐ |
| 非推奨の明示 | 廃止予定がある場合必須 | [14. APIバージョニング](14-api-versioning.md)の廃止プロセスと連動させる |

全フィールドに `@Schema` を書く必要はない。`name`, `id` のように自明なものは省略してよい。

## タグによるグルーピング

Swagger UI上でリソースごとにグルーピングし、管理者向け/顧客向けの境界を分かりやすくする。

```java
@Tag(name = "Admin Products", description = "管理者向け商品管理API")
@RestController
@RequestMapping("/api/admin/products")
public class AdminProductController { ... }
```

## CIでの検証

- `/v3/api-docs` が正常に生成できることをテストで担保する（Controller定義の記述ミスで起動不能になるのを防ぐ）
- 将来的に外部パートナー向けAPIを公開する場合は、OpenAPI定義のLint（`spectral` 等）導入を検討する

---

## チェックリスト

- [ ] 新規エンドポイントに `@Operation(summary = ...)` を付与している
- [ ] 発生しうるビジネス例外(409/404等)を `@ApiResponse` に明記している
- [ ] 単位・意図が自明でないフィールドに `@Schema(description = ...)` を付与している
- [ ] Controllerに `@Tag` を付与し、Swagger UI上でリソースごとにグルーピングされている
