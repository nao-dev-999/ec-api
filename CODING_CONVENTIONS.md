# ec-api コーディング規約

> **対象スタック:** Java 25 / Spring Boot 4.1 / Hibernate (Spring Data JPA) / PostgreSQL / Redis  
> **アーキテクチャ:** レイヤードアーキテクチャ（Controller → Service → Repository）

---

## 目次

1. [パッケージ構成](#1-パッケージ構成)
2. [命名規則](#2-命名規則)
3. [レイヤー別責務](#3-レイヤー別責務)
4. [エンティティ設計](#4-エンティティ設計)（BaseEntity / AuditorAware / 楽観ロック / N+1対策）
5. [DTO設計](#5-dto設計)
6. [例外処理](#6-例外処理)
7. [Spring Security](#7-spring-security)
8. [テスト](#8-テスト)
9. [データベース / Flyway](#9-データベース--flyway)
10. [フォーマット](#10-フォーマット)
11. [エラーコード・メッセージ管理](#11-エラーコードメッセージ管理)

---

## 1. パッケージ構成

```
com.example.ecapi
├── config/           # Bean定義・設定クラス（Security, Redis, MVC, AOP等）
├── constant/         # Enum・定数
├── controller/
│   ├── admin/        # 管理者向けエンドポイント
│   │   ├── dto/      # リクエスト/レスポンスDTO
│   │   └── mapper/   # DTO ↔ ServiceDTO 変換
│   └── customer/     # 顧客向けエンドポイント
│       ├── dto/
│       └── mapper/
├── entity/           # JPAエンティティ
├── exception/        # 例外クラス・GlobalExceptionHandler
├── helper/           # ユーティリティ（MessageHelper等）
├── repository/       # Spring Data JPAリポジトリ
└── service/
    ├── auth/
    ├── order/
    │   ├── dto/      # サービス層の入出力オブジェクト
    │   └── mapper/   # Entity ↔ ServiceDTO 変換
    └── product/
        ├── dto/
        └── mapper/
```

**原則:**

- `controller/dto/` はHTTP層の関心事（バリデーション・シリアライズ）のみを持つ
- `service/dto/` はビジネスロジックの入出力を表現する。HTTPに依存しない
- `entity/` はJPA管理下のオブジェクト。SpringやHTTPに依存しない
- `config/` に入るのは `@Bean` / `@Configuration` / `@Aspect` のみ。ビジネスロジックを置かない

---

## 2. 命名規則

### クラス名

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

### メソッド名

| 操作 | 規則 | 例 |
|------|------|----|
| 単一取得 | `get{Resource}` | `getProduct`, `getOrder` |
| 一覧取得 | `list{Resource}s` | `listProducts`, `listOrders` |
| 作成 | `create{Resource}` | `createProduct` |
| 更新 | `update{Resource}` | `updateProduct` |
| 削除 | `delete{Resource}` | `deleteProduct` |
| キャンセル等の状態遷移 | 動詞 | `cancel`, `ship`, `complete` |

### フィールド名

- `camelCase` を使用する
- `boolean` フィールドは `is` プレフィックスを付けない（JPA/Jackson の相互作用を避けるため）
  - ❌ `isDeleted` → ✅ `deleted`
  - ただし DB カラム名は `is_deleted` を許容する（`@Column(name = "is_deleted")`）

---

## 3. レイヤー別責務

### Controller

```java
// ✅ Controllerの責務: HTTP入力受け取り → ServiceDTOに変換 → Service呼び出し → レスポンスDTO返却
@RestController
@RequestMapping("/api/admin/products")
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_PRODUCT_MANAGER')")
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

### Service

```java
// ✅ Serviceの責務: ビジネスロジック。Spring SecurityもHTTPも知らない
@Service
@Transactional
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductEntityMapper mapper;
    private final MessageHelper messageHelper;

    public ProductService(ProductRepository productRepository,
                          ProductEntityMapper mapper,
                          MessageHelper messageHelper) {
        this.productRepository = productRepository;
        this.mapper = mapper;
        this.messageHelper = messageHelper;
    }

    @Transactional(readOnly = true)
    public ProductResult getProduct(Long id) {
        var product = productRepository.findById(id)
            .orElseThrow(() -> new ProductNotFoundException(
                messageHelper.getMessage("error.product.not_found", id)));
        return mapper.toProductResult(product);
    }
}
```

**原則:**

- クラスレベルに `@Transactional` を付け、読み取り専用メソッドには `@Transactional(readOnly = true)` を付ける
- `SecurityContextHolder` / `Principal` を参照しない（引数で受け取る）
- インターフェースは「複数実装が存在する理由がある場合」のみ定義する。`ServiceImpl` パターンは採用しない

### Repository

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

---

## 4. エンティティ設計

### BaseEntity

監査カラム（作成日時・更新日時・作成者・更新者）は全テーブル共通のため `BaseEntity` に集約し、継承する。

```java
// entity/BaseEntity.java
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @Version
    @Column(nullable = false)
    private Long version;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;

    @CreatedBy
    @Column(nullable = false, updatable = false)
    private Long createdBy;

    @LastModifiedBy
    @Column(nullable = false)
    private Long updatedBy;

    // getter のみ（setter は公開しない。JPA Auditing が自動セットする）
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Long getCreatedBy() { return createdBy; }
    public Long getUpdatedBy() { return updatedBy; }
}
```

**原則:**

- `@MappedSuperclass` を使う（`@Inheritance` ではない。テーブルを分けない）
- `setter` は公開しない。値のセットは JPA Auditing のみが行う
- `createdBy` / `updatedBy` は `Long`（ユーザID）を格納する。ユーザ名等の可変値は持たない

### AuditorAware

`@CreatedBy` / `@LastModifiedBy` に何をセットするかは `AuditorAware` が決定する。
**`SecurityContextHolder` を触るのはこのクラスのみ**とし、Service・Repository からは参照しない。

```java
// config/JpaAuditConfig.java
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class JpaAuditConfig {

    @Bean
    public AuditorAware<Long> auditorAware() {
        return () ->
            Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .filter(Authentication::isAuthenticated)
                .filter(auth -> auth.getPrincipal() instanceof UserDetails)
                .map(auth -> ((LoginUserDetails) auth.getPrincipal()).getUserId());
    }
}
```

```java
// セッションに格納するユーザ情報
public class LoginUserDetails implements UserDetails {

    private final Long userId;      // DB の主キー（createdBy / updatedBy に使う）
    private final String username;
    private final Collection<? extends GrantedAuthority> authorities;

    public LoginUserDetails(Long userId, String username,
                            Collection<? extends GrantedAuthority> authorities) {
        this.userId = userId;
        this.username = username;
        this.authorities = authorities;
    }

    public Long getUserId() { return userId; }

    // UserDetails の実装は省略
}
```

**バッチ・非同期処理での注意:**

- バッチ起動時は `SecurityContext` が空のため `AuditorAware` が `Optional.empty()` を返す
- `createdBy` / `updatedBy` をバッチでも必須にする場合は、バッチ専用のシステムユーザ（例: `id = 0`）を DB に用意し、バッチ起動処理で `SecurityContext` にセットする
- `@Async` メソッドでは親スレッドの `SecurityContext` が引き継がれないため、`SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL)` を設定する

### 基本構造

```java
@Entity
@Table(name = "products")
public class Product extends BaseEntity {   // ← BaseEntity を継承

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false)
    private Integer price;

    @Column(nullable = false)
    private Integer stock;

    @Column(nullable = false)
    private boolean deleted = false;

    // JPA必須
    protected Product() {}

    // ファクトリメソッドで生成を統制する
    public static Product create(String name, Integer price, Integer stock) {
        var product = new Product();
        product.name = name;
        product.price = price;
        product.stock = stock;
        return product;
    }

    // getter / setter
}
```

**禁止事項:**

- `@Builder` をエンティティに付けない（`@Builder.Default` の罠と `@NoArgsConstructor` の競合）
- `Lombok @Data` をエンティティに付けない（`hashCode`/`equals` が id のみで動作せず、LazyLoading で問題発生）
- `@OneToMany` のデフォルトFetchTypeを変えない（`LAZY` のまま使う）
- `createdBy` / `updatedBy` を Service や Controller から手動でセットしない（`AuditorAware` に任せる）

### 時刻

| 用途 | 型 | DBカラム型 |
|------|----|----------|
| 作成日時・更新日時 | `Instant` | `TIMESTAMPTZ` |
| 予約日・誕生日等の「日付」 | `LocalDate` | `DATE` |
| タイムゾーンを意識した表示 | `ZonedDateTime`（変換時のみ） | — |

- `TIMESTAMP`（タイムゾーンなし）は使用しない。`TIMESTAMPTZ` に統一する
- `Instant` はエポック秒ではなく、HibernateがJDBCで `TIMESTAMPTZ` にマッピングする

### 論理削除

- `deleted boolean DEFAULT false` カラムで管理する
- Repository のクエリには必ず `AND deleted = false` を含める
- 物理削除は外部キー参照のある親レコードには使用しない

### 楽観ロック

**`version` カラムは `BaseEntity` に定義するため全テーブルに一律付与する。**  
楽観ロックチェックを実際に「効かせるかどうか」は操作の内容に応じて判断する。

#### 仕組み

Hibernate はエンティティ更新時に `WHERE id = ? AND version = ?` を自動付与し、バージョンをインクリメントする。0件更新になった場合（他トランザクションが先に更新済み）は `OptimisticLockException` をスローする。

```sql
-- Hibernate が自動生成する UPDATE
UPDATE products
SET stock = 7, version = 2, updated_at = NOW()
WHERE id = 1 AND version = 1;  -- 競合時は 0件更新 → OptimisticLockException
```

#### 楽観ロックを「効かせる」操作

クライアントから受け取った `version` をエンティティにセットする。これにより取得時と更新時の間に他ユーザが更新していた場合に `409 Conflict` を返せる。

```java
// Service
public ProductResult updateProduct(Long id, UpdateProduct command) {
    var product = productRepository.findByIdAndDeletedFalse(id)
        .orElseThrow(() -> new ProductNotFoundException(...));

    product.setVersion(command.version()); // ← クライアントのversionをセット
    product.setName(command.name());
    product.setPrice(command.price());

    return mapper.toProductResult(product); // flush時にバージョンチェックが走る
}
```

```java
// Service DTO
public record UpdateProduct(
    Long version,   // クライアントから受け取る
    String name,
    Integer price
) {}
```

#### 楽観ロックを「効かせない」操作

`setVersion()` を呼ばなければ Hibernate は現在の version をそのまま使うため、常に最新状態に上書きされる。管理者による強制上書きや、バッチによる一括更新がこれにあたる。

```java
// 楽観ロックなし（管理者の強制上書き）
product.setName(command.name());
// setVersion() を呼ばない → 競合チェックなしで更新
```

#### 楽観ロックが特に重要な操作

| 操作 | 理由 |
|------|------|
| 在庫の増減 | 同時注文によるロストアップデートが発生しうる |
| 注文ステータス遷移 | キャンセルと出荷が競合する可能性がある |
| 顧客情報の更新 | 複数端末から同時編集される可能性がある |

#### API への影響

レスポンス DTO に `version` を含め、更新リクエスト DTO でも受け取る。

```json
// GET レスポンス
{ "id": 1, "name": "商品A", "price": 1000, "version": 3 }

// PUT リクエスト（取得時の version をそのまま送る）
{ "name": "商品A改", "price": 1200, "version": 3 }
```

### 4.4 データアクセス・フェッチ戦略（N+1問題の対策）

#### 背景：なぜN+1問題が起きるのか

JPAは「RDBのテーブル構造をJavaのオブジェクト参照に翻訳するマシン」である。RDBの世界ではテーブル間の繋がりは「外部キーの値」だが、Javaでは「オブジェクトへのポインタ参照」として扱う。このギャップを埋めるためにJPAはリレーション定義を要求する。

デフォルトのフェッチ戦略は **Lazy Loading**（遅延フェッチ）であり、関連オブジェクトは「実際にアクセスされた瞬間」に初めてDBへ取得しにいく。これはメモリ枯渇（OOM）を防ぐ安全弁だが、ループ処理と組み合わさると **N+1問題** を引き起こす。

```
// 親1件を取得（1クエリ）→ ループ内で子にアクセス（N クエリ）= 合計 N+1 クエリ
orders.forEach(order -> order.getDetails().size());  // N回の追加クエリが発生
```

#### 防御策1: グローバルセーフティネット

予期しないN+1が発生しても爆発的なクエリ増加を抑えるため、`application.yml` に一括フェッチサイズを設定する。これにより万が一N+1が発生しても、100件ずつのIN句クエリに自動最適化される。

```yaml
spring:
  jpa:
    properties:
      hibernate:
        default_batch_fetch_size: 100
        generate_statistics: true  # パフォーマンス検証用（本番では削除推奨）
```

#### 防御策2: Repository層でのフェッチ制御

| 方法 | 使用基準 |
|------|---------|
| `@EntityGraph` | 単純な関連エンティティの一括結合。ページネーション（`Pageable`）を伴う検索 |
| `JOIN FETCH`（JPQL） | 結合条件に動的な絞り込みが必要な場合。DTOへの直接マッピングを行う複雑なクエリ |

```java
public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {

    // パターンA: @EntityGraph（推奨。ページネーション対応）
    @EntityGraph(attributePaths = {"details", "customer"})
    Page<CustomerOrder> findAll(Pageable pageable);

    // パターンB: JOIN FETCH（複雑な条件絞り込み）
    @Query("""
        SELECT o FROM CustomerOrder o
        JOIN FETCH o.details d
        JOIN FETCH o.customer c
        WHERE o.id = :id AND o.deleted = false
    """)
    Optional<CustomerOrder> findByIdFetchDetails(@Param("id") Long id);
}
```

#### 防御策3: バルク演算（`@Modifying`）の制約

`@Modifying` による一括更新・削除はHibernateの永続化コンテキスト（1次キャッシュ）をバイパスして直接DBにSQLを発行する。以下の2点を必ず守る。

**① `clearAutomatically = true` を付ける** — バルク演算後に永続化コンテキストをクリアし、次回参照時にDBから最新状態を再取得させる。

**② `version` を手動インクリメントする** — バルク演算では `@Version` の自動インクリメントが効かないため、クエリ内で明示的に更新する。

```java
@Modifying(clearAutomatically = true)
@Query("""
    UPDATE Product p
    SET p.price = p.price - :discount,
        p.version = p.version + 1
    WHERE p.category = :category AND p.deleted = false
""")
int bulkDiscountByCategory(@Param("category") String category,
                           @Param("discount") int discount);
```

#### `@OneToMany` 双方向マッピングの制御

親子間で双方向関連を持つ場合、Javaメモリ上の状態とRDBの外部キー状態を常に一致させる。

**必須要件:**

- `@OneToMany` 側（親）には必ず `mappedBy` を指定する（外部キーの管理権限が子側にあることを明示）
- 子コレクションのgetterは `Collections.unmodifiableList` で返し、外部からの直接操作を防ぐ
- 親エンティティに `addXxx` / `removeXxx` ヘルパーメソッドを定義し、Service層はこれのみを使う

```java
@Entity
public class CustomerOrder extends BaseEntity {

    @OneToMany(mappedBy = "customerOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CustomerOrderDetail> details = new ArrayList<>();

    // ✅ ヘルパーメソッド経由で追加（親子のポインタをアトミックに同期）
    public void addDetail(CustomerOrderDetail detail) {
        this.details.add(detail);
        detail.setCustomerOrder(this);
    }

    public void removeDetail(CustomerOrderDetail detail) {
        if (this.details.remove(detail)) {
            detail.setCustomerOrder(null);
        }
    }

    // ✅ 読み取り専用で返す（外部からの getDetails().add(...) を防ぐ）
    public List<CustomerOrderDetail> getDetails() {
        return Collections.unmodifiableList(this.details);
    }
}
```

```java
// 子エンティティ側（外部キーの管理権限を持つ）
@Entity
public class CustomerOrderDetail {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_order_id", nullable = false)
    private CustomerOrder customerOrder;

    // パッケージプライベートに制限（ヘルパー経由以外での書き換えを防ぐ）
    void setCustomerOrder(CustomerOrder customerOrder) {
        this.customerOrder = customerOrder;
    }
}
```

```java
// Service層での使い方
@Transactional
public Long createOrder(CreateOrder command) {
    var order = new CustomerOrder();

    command.items().forEach(item -> {
        var detail = new CustomerOrderDetail();
        detail.setProductId(item.productId());
        detail.setQuantity(item.quantity());

        // ❌ order.getDetails().add(detail)  → UnsupportedOperationException
        order.addDetail(detail);  // ✅ ヘルパー経由
    });

    // CascadeType.ALL により子明細も同一トランザクションで一括Insert
    return orderRepository.save(order).getId();
}
```

---

## 5. DTO設計

### バリデーション

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

### マッパー

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

---

## 6. 例外処理

### 設計方針

- ビジネス例外は共通基底クラス `BusinessException` を継承する
- `GlobalExceptionHandler` は `BusinessException` を一括処理する。例外の種類ごとに `@ExceptionHandler` を増やさない
- Spring Security の 401/403 は `@ControllerAdvice` で処理できない。`SecurityConfig` の `exceptionHandling()` で設定する
- 500 エラーはスタックトレースをログに必ず残す。レスポンスには内部情報を含めない
- メッセージは `messages_{locale}.properties` で管理する（詳細は11章参照）

### ErrorCode Enum

エラーコードと HTTP ステータスの対応を一元管理する。

```java
// exception/ErrorCode.java
public enum ErrorCode {

    // 認証・認可
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED),
    FORBIDDEN(HttpStatus.FORBIDDEN),

    // 商品
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND),
    INSUFFICIENT_STOCK(HttpStatus.UNPROCESSABLE_ENTITY),

    // 注文
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND),
    ORDER_ALREADY_CANCELLED(HttpStatus.CONFLICT),

    // 汎用
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST),
    CONFLICT(HttpStatus.CONFLICT),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

    private final HttpStatus status;

    ErrorCode(HttpStatus status) {
        this.status = status;
    }

    public HttpStatus getStatus() { return status; }
}
```

**命名規則:** `{RESOURCE}_{REASON}` の形式。可読性を優先し、コードだけで意味がわかるようにする。

### BusinessException（基底例外クラス）

```java
// exception/BusinessException.java
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;
    private final Object[] args;  // messages.properties の埋め込みパラメータ

    public BusinessException(ErrorCode errorCode, Object... args) {
        super(errorCode.name());
        this.errorCode = errorCode;
        this.args = args;
    }

    public ErrorCode getErrorCode() { return errorCode; }
    public Object[] getArgs() { return args; }
}
```

### 個別例外クラス

`BusinessException` を継承し、引数でパラメータを受け取る。メッセージ文字列は持たない。

```java
// exception/ProductNotFoundException.java
public class ProductNotFoundException extends BusinessException {
    public ProductNotFoundException(Long id) {
        super(ErrorCode.PRODUCT_NOT_FOUND, id);
    }
}

// exception/InsufficientStockException.java
public class InsufficientStockException extends BusinessException {
    public InsufficientStockException(Long productId, int requested, int available) {
        super(ErrorCode.INSUFFICIENT_STOCK, productId, requested, available);
    }
}

// exception/OrderAlreadyCancelledException.java
public class OrderAlreadyCancelledException extends BusinessException {
    public OrderAlreadyCancelledException(Long orderId) {
        super(ErrorCode.ORDER_ALREADY_CANCELLED, orderId);
    }
}
```

### ErrorResponse

```java
// exception/ErrorResponse.java
public record ErrorResponse(
    String code,        // ErrorCode.name()
    String message,     // ロケール解決済みのメッセージ
    String traceId,     // MDC から取得。問い合わせ対応の手がかり
    String path,        // リクエストパス
    List<FieldError> errors  // バリデーションエラー時のみ使用（それ以外は null）
) {
    // バリデーションエラー以外
    public static ErrorResponse of(String code, String message,
                                   String traceId, String path) {
        return new ErrorResponse(code, message, traceId, path, null);
    }

    // バリデーションエラー
    public static ErrorResponse ofValidation(String message,
                                             String traceId, String path,
                                             List<FieldError> errors) {
        return new ErrorResponse(
            ErrorCode.VALIDATION_ERROR.name(), message, traceId, path, errors);
    }

    public record FieldError(String field, String message) {}
}
```

### GlobalExceptionHandler

```java
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final MessageSource messageSource;

    // ビジネス例外を一括処理（個別の @ExceptionHandler を増やさない）
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(
            BusinessException ex, HttpServletRequest request, Locale locale) {

        var errorCode = ex.getErrorCode();
        var message = messageSource.getMessage(errorCode.name(), ex.getArgs(), locale);
        var traceId = MDC.get("traceId");

        // ビジネス例外はスタックトレース不要。コード・パラメータのみWARNログ
        log.warn("[{}] {} args={}", traceId, errorCode, Arrays.toString(ex.getArgs()));

        return ResponseEntity
            .status(errorCode.getStatus())
            .body(ErrorResponse.of(errorCode.name(), message, traceId,
                                   request.getRequestURI()));
    }

    // バリデーションエラー
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request, Locale locale) {

        var traceId = MDC.get("traceId");
        var message = messageSource.getMessage(
            ErrorCode.VALIDATION_ERROR.name(), null, locale);
        var fieldErrors = ex.getBindingResult().getFieldErrors().stream()
            .map(e -> new ErrorResponse.FieldError(e.getField(), e.getDefaultMessage()))
            .toList();

        log.warn("[{}] VALIDATION_ERROR fields={}", traceId,
            fieldErrors.stream().map(ErrorResponse.FieldError::field).toList());

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse.ofValidation(message, traceId,
                                             request.getRequestURI(), fieldErrors));
    }

    // 楽観ロック競合
    @ExceptionHandler(OptimisticLockException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLock(
            OptimisticLockException ex, HttpServletRequest request, Locale locale) {

        var traceId = MDC.get("traceId");
        var message = messageSource.getMessage(ErrorCode.CONFLICT.name(), null, locale);

        log.warn("[{}] CONFLICT optimistic lock", traceId);

        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(ErrorResponse.of(ErrorCode.CONFLICT.name(), message, traceId,
                                   request.getRequestURI()));
    }

    // 予期しない例外（500）
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(
            Exception ex, HttpServletRequest request, Locale locale) {

        var traceId = MDC.get("traceId");
        var message = messageSource.getMessage(
            ErrorCode.INTERNAL_SERVER_ERROR.name(), null, locale);

        // システム例外はスタックトレースをERRORログに残す
        log.error("[{}] Unexpected error", traceId, ex);

        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ErrorResponse.of(
                ErrorCode.INTERNAL_SERVER_ERROR.name(), message, traceId,
                request.getRequestURI()));
    }
}
```

---

## 7. Spring Security

### SecurityConfig の基本方針

```java
// 認可: hasAnyAuthority() を使う（hasAnyRole()はROLE_プレフィックスが二重になるリスクがある）
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/api/admin/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_PRODUCT_MANAGER")
    .requestMatchers("/api/customer/**").hasAuthority("ROLE_CUSTOMER")
    .requestMatchers("/api/auth/**").permitAll()
    .anyRequest().authenticated()
)
```

### メソッドセキュリティ

- クラスレベルの `@PreAuthorize` でロールガードをかける
- メソッドレベルのオーナーチェック（自分の注文のみ操作可能等）には `@PostAuthorize` または Service 内で `customerId` を比較する

```java
// ✅ Controller クラスレベルでロール制限
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
@RestController
public class AdminOrderController { ... }

// ✅ @EnableMethodSecurity が有効であることを SecurityConfig で確認
@Configuration
@EnableMethodSecurity
public class SecurityConfig { ... }
```

### セッション管理（Redis）

- JWT は使用しない（`JwtAuthenticationFilter`, `JwtHelper`, `JwtProperties`, `TokenRedisService` は削除済み）
- Spring Session + Redis でセッション管理
- CSRF は REST API（`Content-Type: application/json` + CORS制御）では無効化可
- Cookie は `SameSite=Lax` で CSRF 保護

---

## 8. テスト

### Controller テスト

```java
@WebMvcTest(ProductController.class)
@Import(SecurityConfig.class)
class ProductControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean ProductService productService;

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void createProduct_shouldReturn201() throws Exception {
        // given
        var result = new ProductResult(1L, "商品A", 1000, 10);
        given(productService.createProduct(any())).willReturn(result);

        // when / then
        mockMvc.perform(post("/api/admin/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":"商品A","price":1000,"stock":10}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1L));
    }
}
```

### Service テスト

```java
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @InjectMocks ProductService productService;
    @Mock ProductRepository productRepository;
    @Mock ProductEntityMapper mapper;
    @Mock MessageHelper messageHelper;

    @Test
    void getProduct_whenNotFound_shouldThrow() {
        given(productRepository.findById(99L)).willReturn(Optional.empty());
        assertThrows(ProductNotFoundException.class,
            () -> productService.getProduct(99L));
    }
}
```

**原則:**

- Controller テストは `@WebMvcTest`（Spring MVC層のみ起動）
- Service テストは `@ExtendWith(MockitoExtension.class)`（Spring context 不要）
- `@SpringBootTest` は統合テスト（E2E）に限定する
- テストメソッド名: `{メソッド名}_{条件}_{期待結果}` （例: `getProduct_whenNotFound_shouldThrow`）

### JaCoCo

```kotlin
// build.gradle.kts
jacoco {
    toolVersion = "0.8.14"  // Java 25 対応（0.8.12 は非対応）
}
```

---

## 9. データベース / Flyway

### マイグレーションファイル命名

```
V{version}__{description}.sql
```

- description はスネークケース
- 例: `V1__initial_schema.sql`, `V4__add_employee.sql`

### DDL 規約

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

---

## 10. フォーマット

### Spotless / Google Java Format

```kotlin
// build.gradle.kts
spotless {
    java {
        googleJavaFormat()
        importOrder()
        removeUnusedImports()
    }
}
```

**import 順序（Google Java Format 準拠）:**

1. 通常 import をアルファベット順で1グループ
2. 空行
3. static import をアルファベット順で1グループ

```java
// ✅ 正しい順序
import com.example.ecapi.entity.Product;
import org.springframework.web.bind.annotation.RestController;
import java.time.Instant;

import static org.mockito.BDDMockito.given;
```

### その他

- インデントは4スペース（Spotlessが強制）
- 行末の空白は不可（Spotlessが強制）
- 1行の最大文字数は100文字（Google Java Format デフォルト）
- `var` は型が明らかな場合に使用する（`var list = new ArrayList<String>()` は可、`var x = someMethod()` は型が不明瞭なら避ける）

---

## 11. エラーコード・メッセージ管理

### 設計方針

| 関心事 | 担当 |
|--------|------|
| エラーの種別・HTTPステータス | `ErrorCode` Enum |
| ユーザ向けメッセージ | `messages_{locale}.properties` |
| 問い合わせ対応・原因特定 | `traceId` + サーバログ |

### エラーコード命名規則

`{RESOURCE}_{REASON}` の形式で統一する。

```
// ✅ リソースと理由が明確
PRODUCT_NOT_FOUND
INSUFFICIENT_STOCK
ORDER_ALREADY_CANCELLED
CUSTOMER_NOT_FOUND

// ❌ 汎用すぎて意味が不明
NOT_FOUND
ERROR_001
FAILED
```

### messages.properties のキー体系

`ErrorCode.name()` をそのままキーにする。コードとメッセージが一対一で対応し、管理しやすい。

```properties
# messages_ja.properties（日本語・デフォルト）
PRODUCT_NOT_FOUND=商品が見つかりません（id\={0}）
INSUFFICIENT_STOCK=在庫が不足しています（商品id\={0}、要求数\={1}、在庫数\={2}）
ORDER_NOT_FOUND=注文が見つかりません（id\={0}）
ORDER_ALREADY_CANCELLED=この注文はすでにキャンセル済みです（id\={0}）
VALIDATION_ERROR=入力値が不正です
CONFLICT=他のユーザによって更新されています。最新の情報を取得して再度お試しください
UNAUTHORIZED=認証が必要です
FORBIDDEN=この操作を行う権限がありません
INTERNAL_SERVER_ERROR=予期しないエラーが発生しました
```

```properties
# messages_en.properties（英語）
PRODUCT_NOT_FOUND=Product not found (id\={0})
INSUFFICIENT_STOCK=Insufficient stock (productId\={0}, requested\={1}, available\={2})
ORDER_NOT_FOUND=Order not found (id\={0})
ORDER_ALREADY_CANCELLED=This order has already been cancelled (id\={0})
VALIDATION_ERROR=Validation failed
CONFLICT=The resource was updated by another user. Please reload and try again
UNAUTHORIZED=Authentication required
FORBIDDEN=You do not have permission to perform this action
INTERNAL_SERVER_ERROR=An unexpected error occurred
```

**原則:**

- パラメータは `{0}`, `{1}` の形式で埋め込む（`MessageFormat` 準拠）
- `=` の直後にスペースを入れない
- ユーザに見せる情報のみ記載する。スタックトレース・SQL等の内部情報は含めない

### 多言語対応の設定

```java
// config/MessageConfig.java
@Configuration
public class MessageConfig {

    @Bean
    public MessageSource messageSource() {
        var source = new ReloadableResourceBundleMessageSource();
        source.setBasenames("classpath:messages");
        source.setDefaultEncoding("UTF-8");
        source.setDefaultLocale(Locale.JAPANESE);  // フォールバック
        source.setCacheSeconds(3600);
        return source;
    }

    // Accept-Language ヘッダーからロケールを解決する
    @Bean
    public LocaleResolver localeResolver() {
        var resolver = new AcceptHeaderLocaleResolver();
        resolver.setSupportedLocales(List.of(Locale.JAPANESE, Locale.ENGLISH));
        resolver.setDefaultLocale(Locale.JAPANESE);
        return resolver;
    }
}
```

クライアントは `Accept-Language` ヘッダーでロケールを指定する。

```
Accept-Language: ja   →  messages_ja.properties
Accept-Language: en   →  messages_en.properties
（未対応のロケール）   →  messages_ja.properties（デフォルト）
```

### traceId による問い合わせ対応

#### traceId の生成とMDCへのセット

リクエスト開始時にフィルタで生成し、レスポンス終了時にクリアする。

```java
// config/TraceIdFilter.java
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter implements Filter {

    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String MDC_KEY = "traceId";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {
        var httpRequest = (HttpServletRequest) request;
        var httpResponse = (HttpServletResponse) response;

        // クライアントからtraceIdが来ればそれを使う（外部システム連携）
        // なければサーバ側で生成する
        var traceId = Optional.ofNullable(httpRequest.getHeader(TRACE_ID_HEADER))
            .filter(id -> !id.isBlank())
            .orElse(UUID.randomUUID().toString());

        MDC.put(MDC_KEY, traceId);
        httpResponse.setHeader(TRACE_ID_HEADER, traceId);  // レスポンスにも返す

        try {
            chain.doFilter(request, response);
        } finally {
            MDC.clear();  // スレッドプールへの返却前に必ずクリア
        }
    }
}
```

#### logback の設定

```xml
<!-- logback-spring.xml -->
<pattern>%d{yyyy-MM-dd HH:mm:ss} [%X{traceId}] %-5level %logger{36} - %msg%n</pattern>
```

ログ出力例:

```
2026-06-27 10:00:00 [550e8400-e29b-41d4-a716-446655440000] WARN  OrderService - [550e8400] INSUFFICIENT_STOCK args=[5, 10, 3]
2026-06-27 10:00:00 [550e8400-e29b-41d4-a716-446655440000] ERROR GlobalExceptionHandler - [550e8400] Unexpected error
  java.lang.NullPointerException: ...
```

#### エラーレスポンスへの含め方

```json
{
  "code": "INSUFFICIENT_STOCK",
  "message": "在庫が不足しています（商品id=5、要求数=10、在庫数=3）",
  "traceId": "550e8400-e29b-41d4-a716-446655440000",
  "path": "/api/orders"
}
```

問い合わせ対応フロー:

```
1. ユーザから「エラーが出た」と問い合わせが来る
2. 「画面に表示されている traceId を教えてください」と依頼する
3. traceId でサーバログを検索する
4. 該当ログからエラーコード・パラメータ・スタックトレースを確認する
```

#### ビジネス例外とシステム例外のログレベル使い分け

| 例外の種類 | ログレベル | スタックトレース | 理由 |
|-----------|-----------|----------------|------|
| `BusinessException` | WARN | 出さない | 想定内の例外。コード・パラメータで十分特定できる |
| `OptimisticLockException` | WARN | 出さない | 競合は想定内 |
| `Exception`（予期しない） | ERROR | 出す | 想定外。詳細調査が必要 |

### 新しいエラーコードを追加するときの手順

1. `ErrorCode` Enum に追加する（HTTPステータスを必ず付ける）
2. `messages_ja.properties` にメッセージを追加する
3. `messages_en.properties` にメッセージを追加する
4. 個別例外クラスが必要なら `BusinessException` を継承して作成する
5. `GlobalExceptionHandler` は**触らない**（`BusinessException` を一括処理するため）

### PR レビュー前に確認

- [ ] `./gradlew spotlessCheck` がパス
- [ ] `./gradlew test` がパス
- [ ] 新しいエンドポイントに認可アノテーションが付いている
- [ ] 新しい Flyway マイグレーションファイルのバージョンが連番
- [ ] エンティティに `@Builder` が付いていない
- [ ] Service が `SecurityContextHolder` を参照していない
- [ ] `createdBy` / `updatedBy` を手動セットしていない（`AuditorAware` に任せる）
- [ ] 在庫・ステータス遷移等の更新処理でクライアントから `version` を受け取っている
- [ ] 読み取りメソッドに `@Transactional(readOnly = true)` が付いている
- [ ] 一覧取得で関連エンティティを使う場合に `@EntityGraph` または `JOIN FETCH` を使っている
- [ ] `@Modifying` に `clearAutomatically = true` が付いている
- [ ] `@OneToMany` の子コレクション操作がヘルパーメソッド経由になっている
- [ ] 例外メッセージが `messages.properties` 経由
- [ ] 新しいエラーコードを `ErrorCode` Enum・`messages_ja.properties`・`messages_en.properties` の3箇所に追加している
- [ ] `GlobalExceptionHandler` に個別の `@ExceptionHandler` を追加していない（`BusinessException` 一括処理）
