# 5. DTO設計

> [← インデックスに戻る](../コーディング規約.md)

---

## バリデーション

```java
// Controller層のRequest DTO
public record CreateProductRequest(
    @NotBlank @Size(max = 255) String name,
    @NotNull @Min(0) Integer price,
    @NotNull @Min(0) Integer stock
) {}
```

- バリデーションアノテーションは Controller 層の DTO にのみ付ける
- Service 層の DTO にはバリデーションアノテーションを付けない（Service は既に検証済みの入力を受け取る前提）
- `record` を DTO に使用することを推奨する（不変性・簡潔さ）

---

## マッパー

```java
// Controller層のマッパー: RequestDTO ↔ ServiceDTO
@Component
public class ProductApiMapper {
    public CreateProduct toCreateProduct(CreateProductRequest req) {
        return new CreateProduct(req.name(), req.price(), req.stock());
    }
    public ProductResponse toProductResponse(ProductResult result) {
        return new ProductResponse(result.id(), result.name(), result.price(), result.stock());
    }
}

// Service層のマッパー: ServiceDTO ↔ Entity
@Component
public class ProductEntityMapper {
    public ProductResult toProductResult(Product product) {
        return new ProductResult(product.getId(), product.getName(),
                                 product.getPrice(), product.getStock());
    }
}
```

- MapStructは使用しない（Spring Boot 4 + Java record との相性を慎重に評価するまで）
- マッパーは `@Component` の POJO。ロジックを持たない変換のみ

### マッパーを切り出す基準

| 状況 | 方針 |
|------|------|
| 同じ変換ロジックが複数のクラスで使われる | `@Component` マッパークラスに切り出す |
| 1つのクラスでしか使わない | コントローラーのプライベートメソッドで可 |
