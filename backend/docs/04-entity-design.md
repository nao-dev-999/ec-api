# 4. エンティティ設計

> [← インデックスに戻る](../コーディング規約.md)

---

## 4.1 BaseEntity

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

---

## 4.2 AuditorAware

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

---

## 4.3 エンティティの基本構造

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

---

## 4.4 時刻

| 用途 | 型 | DBカラム型 |
|------|----|----------|
| 作成日時・更新日時 | `Instant` | `TIMESTAMPTZ` |
| 予約日・誕生日等の「日付」 | `LocalDate` | `DATE` |
| タイムゾーンを意識した表示 | `ZonedDateTime`（変換時のみ） | — |

- `TIMESTAMP`（タイムゾーンなし）は使用しない。`TIMESTAMPTZ` に統一する
- `Instant` はエポック秒ではなく、HibernateがJDBCで `TIMESTAMPTZ` にマッピングする

---

## 4.5 論理削除

- `deleted boolean DEFAULT false` カラムで管理する
- Repository のクエリには必ず `AND deleted = false` を含める
- 物理削除は外部キー参照のある親レコードには使用しない

---

## 4.6 楽観ロック

**`version` カラムは `BaseEntity` に定義するため全テーブルに一律付与する。**
楽観ロックチェックを実際に「効かせるかどうか」は操作の内容に応じて判断する。

### 仕組み

Hibernate はエンティティ更新時に `WHERE id = ? AND version = ?` を自動付与し、バージョンをインクリメントする。0件更新になった場合（他トランザクションが先に更新済み）は `OptimisticLockException` をスローする。

```sql
-- Hibernate が自動生成する UPDATE
UPDATE products
SET stock = 7, version = 2, updated_at = NOW()
WHERE id = 1 AND version = 1;  -- 競合時は 0件更新 → OptimisticLockException
```

### 楽観ロックを「効かせる」操作

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

### 楽観ロックを「効かせない」操作

`setVersion()` を呼ばなければ Hibernate は現在の version をそのまま使うため、常に最新状態に上書きされる。管理者による強制上書きや、バッチによる一括更新がこれにあたる。

```java
// 楽観ロックなし（管理者の強制上書き）
product.setName(command.name());
// setVersion() を呼ばない → 競合チェックなしで更新
```

### 削除（DELETE）操作における楽観ロック

**本プロジェクトの DELETE 操作では楽観ロックを適用しない。**

削除はレコードの存在自体を消す明確な意思を持った操作であり、取得後に他のユーザが更新していたとしても削除の意図は変わらない。更新操作で問題になるロストアップデート（自分の変更が他者の変更を上書きする）は削除では発生しないため、`version` の受け渡しは不要とする。

| 操作 | 楽観ロック | 理由 |
|------|-----------|------|
| UPDATE | **必要** | 自分の変更が他者の変更をロストアップデートする可能性がある |
| DELETE | **不要** | 削除の意図はバージョンに関わらず成立する |

DELETE API は `version` を受け取らず、ID のみで削除する。

### 楽観ロックが特に重要な操作

| 操作 | 理由 |
|------|------|
| 在庫の増減 | 同時注文によるロストアップデートが発生しうる |
| 注文ステータス遷移 | キャンセルと出荷が競合する可能性がある |
| 顧客情報の更新 | 複数端末から同時編集される可能性がある |

### API への影響

レスポンス DTO に `version` を含め、更新リクエスト DTO でも受け取る。

```json
// GET レスポンス
{ "id": 1, "name": "商品A", "price": 1000, "version": 3 }

// PUT リクエスト（取得時の version をそのまま送る）
{ "name": "商品A改", "price": 1200, "version": 3 }
```

---

## 4.7 データアクセス・フェッチ戦略（N+1問題の本質的対策）

### 背景思想：なぜJPAはオブジェクトのリレーションを持つのか

生のSQLやMyBatis等では、テーブル間の繋がりは単なる「外部キーの値」であり、人間がその都度必要な結合（JOIN）を手動で組み立てるため、JPAのような複雑なオブジェクト間マッピング管理は不要でした。

しかし、JPAは単なる「SQLの実行マシン」ではなく、**「RDBのテーブル構造を、Javaの『完全なオブジェクト指向（メモリ上のポインタ参照）』に翻訳・自動同期するマシン」**です。

| RDB（SQL）の世界 | Java（オブジェクト指向）の世界 |
|---|---|
| 繋がりは「値（外部キー）」。方向性はなく、人間がJOINして初めて繋がる。 | 繋がりは「ポインタ（参照）」。親が子のリストを持ち、子が親のインスタンスを持つ。 |

この「値しか信じないRDB」と「参照しか信じないJava」のギャップ（インピーダンスミスマッチ）を埋め、Javaのオブジェクトを操作するだけで裏側で安全にSQLを自動生成するために、JPAは明確なリレーション定義（`mappedBy` や双方向同期ヘルパー）を開発者に要求します。

### Lazy Loading（遅延フェッチ）の存在理由

JPAのデフォルトのフェッチ戦略は、関連オブジェクトを「必要になるまでロードしない」**Lazy Loading**になっています。これは開発者を困らせるためではなく、**アプリケーションのメモリ枯渇（Out Of Memory: OOM）を防ぐための絶対的な安全弁**です。

もし全てが `Eager Loading`（即時フェッチ）の世界だった場合、「会員一覧を10件表示したい」というだけの処理であっても、JPAはオブジェクトツリーを完全に復元しようとするため、「会員 ➔ 全注文履歴 ➔ 全注文明細 ➔ 全商品詳細 ➔ 全商品レビュー」と、芋づる式に何万件もの無関係なデータをDBからかき集め、一瞬でサーバーのメモリを食いつぶしてクラッシュさせます。

これを防ぐため、JPAは**「指示がない限り、今リクエストされた主テーブルのデータだけを持ってくる。関連する子供たちは、プログラム上で実際に触られた（`.get()` された）その瞬間に、初めて個別にDBに get しにいく」**という安全策をとっています。

### N+1問題の発生メカニズムと遵守規則

Lazy Loading自体は優れた防御壁ですが、**「ループ処理」と出会ったときに「N+1問題」というパフォーマンス障害を引き起こします。**
1回の親クエリ（1）で取得したN件のレコードに対して、ループ内でLazyな子オブジェクトにアクセスすると、JPAは「今必要になった」と判断して、ループの回数分（N回）の追加クエリを真面目に発行してしまうためです。

この「JPAの自動省メモリ機構（Lazy）」と「開発者のユースケース（まとめて画面に出したい）」のすれ違いを解決するため、本プロジェクトでは以下の**3重の防御策**を義務付けます。

#### 規則1：グローバルなセーフティネット設定

予期せぬLazy Loadingのループによる致命的なクエリ回数の爆発を抑えるため、`application.yml` に一括フェッチサイズを必ず指定する。これにより、万が一N+1が発生しても、100件ずつのIN句クエリに自動最適化され、データベースへの負荷を最小限に抑える。

```yaml
spring:
  jpa:
    properties:
      hibernate:
        default_batch_fetch_size: 100
        # パフォーマンス検証用（本番環境では false または削除を推奨）
        generate_statistics: true
```

#### 規則2：リポジトリ層におけるクエリ制御

- **`@EntityGraph`（推奨）:** 単純な関連エンティティの一括結合、およびページネーション（Pageable）を伴う検索で使用する
- **`JOIN FETCH`（JPQL）:** 結合条件（ON句など）に動的な絞り込みが必要な場合や、DTOへの直接マッピングを行う複雑なクエリで使用する

```java
public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {

    // パターンA: @EntityGraph による宣言的 JOIN FETCH（推奨：ページネーション対応）
    @Override
    @EntityGraph(attributePaths = {"details", "customer"})
    Page<CustomerOrder> findAll(Pageable pageable);

    // パターンB: JPQL による明示的 JOIN FETCH（推奨：複雑な条件絞り込み）
    @Query("""
        SELECT o FROM CustomerOrder o
        JOIN FETCH o.details d
        JOIN FETCH o.customer c
        WHERE o.id = :id AND o.deleted = false
    """)
    Optional<CustomerOrder> findByIdWithDetails(@Param("id") Long id);
}
```

#### 規則3：バルク演算（一括更新・削除）の制限

`@Modifying` を付与した `@Query` による一括更新・削除は、Hibernate のファーストレベルキャッシュをバイパスして直接 DB に SQL が発行される。以下を必須とする。

- **`clearAutomatically = true` の付与:** バルク演算実行後、即座に永続化コンテキストをクリアする
- **楽観ロックバージョンの手動更新:** クエリ内で明示的に `version = version + 1` をインクリメントする

```java
@Modifying(clearAutomatically = true)
@Query("""
    UPDATE Product p
    SET p.price = p.price - :discount,
        p.version = p.version + 1
    WHERE p.category = :category AND p.deleted = false
""")
int bulkDiscountByCategory(@Param("category") String category, @Param("discount") int discount);
```

---

## 4.8 一対多（@OneToMany）双方向マッピングの制御

### 原則

親オブジェクトと子オブジェクトの間で双方向関連を持つ場合、オブジェクト指向的な整合性（Javaメモリ上の状態）と、RDBの外部キー制約の状態を常に完全一致させる設計を強制する。

**必須要件:**

- **関連の所有権の明示:** `@OneToMany` 側（親）には必ず `mappedBy` を指定し、外部キーの管理権限が `@ManyToOne` 側（子）にあることを明示する
- **カプセル化の保護:** 子要素のリストを外部から直接操作（`getDetails().add(...)` など）されるのを防ぐため、ゲッターは `Collections.unmodifiableList` でラップして返却する
- **同期用ヘルパーメソッドの義務化:** 親エンティティ内に `addXxx` / `removeXxx` ヘルパーを必ず定義する。Service 層からはこのヘルパーのみを介して要素の追加・削除を行う

### 実装例（親エンティティ）

```java
@Entity
@Table(name = "customer_order")
public class CustomerOrder extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 所有権は子側にあるため mappedBy を指定
    // 親のライフサイクルと同期させるため CascadeType.ALL, orphanRemoval = true を設定
    @OneToMany(mappedBy = "customerOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CustomerOrderDetail> details = new ArrayList<>();

    public void addDetail(CustomerOrderDetail detail) {
        if (detail == null) throw new IllegalArgumentException("Detail cannot be null");
        this.details.add(detail);
        detail.setCustomerOrder(this); // 子側から見た親のポインタをセット
    }

    public void removeDetail(CustomerOrderDetail detail) {
        if (detail == null) throw new IllegalArgumentException("Detail cannot be null");
        if (this.details.remove(detail)) {
            detail.setCustomerOrder(null);
        }
    }

    // 読み取り専用リストで返却（カプセル化保護）
    public List<CustomerOrderDetail> getDetails() {
        return Collections.unmodifiableList(this.details);
    }
}
```

### 実装例（子エンティティ）

```java
@Entity
@Table(name = "customer_order_detail")
public class CustomerOrderDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 双方向の管理権限（外部キー）を持つ側
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_order_id", nullable = false)
    private CustomerOrder customerOrder;

    @Column(nullable = false)
    private Integer quantity;

    // パッケージプライベートに制限し、外部から不用意に書き換えられないよう制御
    void setCustomerOrder(CustomerOrder customerOrder) {
        this.customerOrder = customerOrder;
    }
}
```

### Service 層での適用パターン

```java
@Transactional
public Long createOrder(CreateOrder input) {
    CustomerOrder order = new CustomerOrder();

    input.items().forEach(item -> {
        CustomerOrderDetail detail = new CustomerOrderDetail();
        detail.setQuantity(item.quantity());

        // ❌ order.getDetails().add(detail) → UnsupportedOperationException
        order.addDetail(detail); // ✅ ヘルパー経由で安全に追加
    });

    // CascadeType.ALL により、親を保存すれば子明細も同一トランザクションで一括 Insert される
    return orderRepository.save(order).getId();
}
```
