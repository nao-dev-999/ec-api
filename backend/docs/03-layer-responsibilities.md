# 3. レイヤー別責務

> [← インデックスに戻る](../コーディング規約.md)

---

## Controller

```java
// ✅ Controllerの責務: HTTP入力受け取り → ServiceDTOに変換 → Service呼び出し → レスポンスDTO返却
@RestController
@RequestMapping("/api/admin/products")
public class AdminProductController {

    private final ProductService productService;
    private final ProductApiMapper mapper;

    // コンストラクタインジェクション（@Autowiredは使わない）
    public AdminProductController(ProductService productService, ProductApiMapper mapper) {
        this.productService = productService;
        this.mapper = mapper;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse create(
            @RequestBody @Validated CreateProductRequest request) {
        var command = mapper.toCreateProduct(request);
        var result = productService.createProduct(command);
        return mapper.toProductResponse(result);
    }
}
```

**禁止事項:**

- `@Repository` / `@Entity` を直接 `@Autowired` しない（Serviceを経由する）
- `SecurityContextHolder` を Controller 以外で使用しない
- ビジネスロジック（在庫チェック、状態遷移等）を Controller に書かない
- `HttpServletRequest` / `HttpServletResponse` を Service に渡さない

---

## Service

```java
// ✅ Serviceの責務: ビジネスロジック。Spring SecurityもHTTPも知らない
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductEntityMapper mapper;
    private final MessageHelper messageHelper;

    public ProductResult getProduct(Long id) {
        var product = productRepository.findByIdAndDeletedFalse(id)
            .orElseThrow(() -> new ProductNotFoundException(
                messageHelper.get("product.notFound", id)));
        return mapper.toProductResult(product);
    }
}
```

**原則:**

- クラスレベルに `@Transactional(readOnly = true)` を付け、書き込みメソッドには `@Transactional` を付ける
- `SecurityContextHolder` / `Principal` を参照しない（引数で受け取る）
- インターフェースは「複数実装が存在する理由がある場合」のみ定義する。`ServiceImpl` パターンは採用しない

---

## Repository

```java
// ✅ 命名はSpring Data規約に従う
public interface ProductRepository extends JpaRepository<Product, Long> {

    // 論理削除フィルタを常に付ける
    Optional<Product> findByIdAndDeletedFalse(Long id);

    // 複雑な検索はSpecificationまたは@Queryを使う
    @Query("SELECT p FROM Product p WHERE p.deleted = false AND (:name IS NULL OR p.name LIKE %:name%)")
    Page<Product> searchProducts(@Param("name") String name, Pageable pageable);
}
```
