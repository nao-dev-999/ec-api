# 13. フィルター

> 本章は [コーディング規約.md](./コーディング規約.md) から分離した独立ドキュメントです。
> 対象スタック: Java 25 / Spring Boot 4.1 / Spring Security / Redis

---

## 目次

- [13.1 責務の分類と使い分け](#121-責務の分類と使い分け)
- [13.2 リクエストトレーシングフィルター](#122-リクエストトレーシングフィルター)
- [13.3 ロギングフィルター](#123-ロギングフィルター)
- [13.4 流入制御フィルター](#124-流入制御フィルター)（レート制限 / メンテナンスモード）
- [13.5 フィルターの登録と実行順序](#125-フィルターの登録と実行順序)

---

## 13.1 責務の分類と使い分け

**何をFilterに書くべきか / 何を書くべきでないか。**

| 関心事 | 実装場所 | 理由 |
|--------|----------|------|
| requestId の付与・MDC設定 | `OncePerRequestFilter` | リクエスト全体を横断する。Spring DI が必要 |
| リクエスト/レスポンスのロギング | `OncePerRequestFilter` | 同上 |
| レート制限（IP・ユーザー単位） | `OncePerRequestFilter` | Spring MVC より前で遮断すべき |
| IPホワイトリスト/ブラックリスト | `OncePerRequestFilter` または `SecurityConfig` | 認証より前に弾く |
| メンテナンスモード（全遮断） | `OncePerRequestFilter` | 全リクエストに横断的に適用 |
| 認証・認可 | `SecurityConfig` | Spring Security の責務 |
| リクエストサイズ制限 | `application.yml` | Spring Boot の組み込み設定で十分 |
| ビジネスロジック（注文上限等） | Service 層 | DBや業務ルールを参照する処理はServiceに置く |
| バリデーション | Controller の `@Validated` | Spring MVC が担う |
| 401/403 のエラーレスポンス | `SecurityConfig.exceptionHandling()` | Filter 内の例外は `@ControllerAdvice` に届かない |

**Filterの基底クラス選択:**

```java
// ✅ Spring Boot プロジェクトでは OncePerRequestFilter を使う
// → 同一リクエスト内で複数回呼び出されることを防ぐ（forward / include 対策）
// → GenericFilterBean は直接使わない
public class RequestTracingFilter extends OncePerRequestFilter { ... }
```

**Filterに書いてよい流入制御の判断基準:**

1. Spring MVC（Controller）より前で判断が完結する — DB やビジネスロジックを参照しない
2. 全エンドポイントに横断的に適用される — 特定リソースのみに適用するなら Interceptor または Service が適切

```java
// ✅ Filter に書く: IPアドレスが1分間に100リクエスト超えたら拒否（横断的・DBなし）
// ❌ Filter に書かない: ユーザーの購入回数が月10回を超えたら拒否（ビジネスルール・DB参照あり → Service）
```

---

## 13.2 リクエストトレーシングフィルター

すべてのリクエストに一意の `requestId` を付与し、MDC に格納してログの追跡を可能にする。

```java
// config/RequestTracingFilter.java
public class RequestTracingFilter extends OncePerRequestFilter {

    private static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final String MDC_REQUEST_ID    = "requestId";
    private static final String MDC_USER_ID       = "userId";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {

        // クライアントから渡された requestId を優先。なければ自前で生成
        String requestId = Optional.ofNullable(request.getHeader(REQUEST_ID_HEADER))
                .filter(s -> !s.isBlank())
                .orElse(UUID.randomUUID().toString());

        try {
            MDC.put(MDC_REQUEST_ID, requestId);
            // レスポンスヘッダーにも返す（クライアントが障害追跡に使える）
            response.setHeader(REQUEST_ID_HEADER, requestId);

            // 認証済みユーザのIDをMDCに追加
            // ✅ SecurityContextHolder の参照は Filter 内では許容する（AuditorAware と同様の理由）
            Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                    .filter(Authentication::isAuthenticated)
                    .filter(a -> a.getPrincipal() instanceof LoginUserDetails)
                    .map(a -> ((LoginUserDetails) a.getPrincipal()).getUserId())
                    .ifPresent(id -> MDC.put(MDC_USER_ID, String.valueOf(id)));

            chain.doFilter(request, response);

        } finally {
            // ✅ finally で MDC を必ずクリアする。スレッドプールの汚染を防ぐ
            MDC.remove(MDC_REQUEST_ID);
            MDC.remove(MDC_USER_ID);
        }
    }
}
```

**MDC をログパターンに組み込む（`logback-spring.xml`）:**

```xml
<pattern>%d{ISO8601} [%X{requestId}] [userId:%X{userId}] %-5level %logger{36} - %msg%n</pattern>
```

**禁止事項:**

```java
// ❌ MDC のクリアを finally の外に書く
chain.doFilter(request, response);
MDC.clear(); // 例外発生時にここに到達しないため、スレッドが汚染される

// ❌ MDC.clear() で全エントリを消す
MDC.clear(); // 他のフィルターや処理がセットしたエントリも消えてしまう

// ✅ 自分がセットしたキーだけを remove() で個別に消す
MDC.remove(MDC_REQUEST_ID);
MDC.remove(MDC_USER_ID);
```

---

## 13.3 ロギングフィルター

リクエスト/レスポンスの概要をログに残す。**ボディの中身はログに出力しない**（個人情報・認証情報の漏洩防止）。

```java
// config/RequestLoggingFilter.java
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {

        long startMs = System.currentTimeMillis();
        try {
            chain.doFilter(request, response);
        } finally {
            long durationMs = System.currentTimeMillis() - startMs;
            // ✅ ボディは出力しない。メソッド・パス・ステータス・処理時間のみ
            log.info("{} {} -> {} ({}ms)",
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    durationMs);
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Actuator・Swagger へのリクエストはログ対象外
        String path = request.getRequestURI();
        return path.startsWith("/actuator")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs");
    }
}
```

**禁止事項:**

```java
// ❌ リクエストボディをそのままログに出力する（パスワード・トークンが露出する）
log.debug("Request body: {}", new String(request.getInputStream().readAllBytes()));

// ❌ レスポンスボディをログに出す（セッションIDやユーザ情報が露出する）
log.debug("Response body: {}", responseBody);

// ✅ メソッド・URI・ステータス・処理時間のみに絞る
```

---

## 13.4 流入制御フィルター

#### レート制限

IP アドレスやユーザー単位でリクエスト数を制限する。ライブラリは **Bucket4j + Redis** を使用する（複数インスタンス間で制限値を共有するため）。

```java
// config/RateLimitingFilter.java
public class RateLimitingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {

        String key = "rate:" + resolveClientKey(request);

        if (rateLimitExceeded(key)) {
            // ✅ 遮断時は chain.doFilter() を呼ばず、ここで処理を終わらせる
            response.setStatus(429); // 429 Too Many Requests
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("""
                    {"status":429,"error":"Too Many Requests","message":"リクエスト数の上限を超えました"}
                    """);
            return;
        }
        chain.doFilter(request, response);
    }

    private String resolveClientKey(HttpServletRequest request) {
        // 認証済みユーザはユーザID単位、未認証はIPアドレス単位で制限
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .filter(Authentication::isAuthenticated)
                .filter(a -> a.getPrincipal() instanceof LoginUserDetails)
                .map(a -> "user:" + ((LoginUserDetails) a.getPrincipal()).getUserId())
                .orElse("ip:" + request.getRemoteAddr());
    }
}
```

**レート制限の適用対象:**

| エンドポイント | 制限の目安 | 理由 |
|---------------|-----------|------|
| `/api/auth/login` | 10回/分・IP単位 | ブルートフォース攻撃の防止 |
| `/api/auth/signup` | 5回/時・IP単位 | スパムアカウント作成の防止 |
| `/api/**`（全体） | 200回/分・ユーザー単位 | 一般的な過負荷対策 |

#### メンテナンスモード

全リクエストを一律で遮断する場合、フラグを **Redis** で管理し、`OncePerRequestFilter` で判定する。アプリを再起動せずに即時切り替えられることを優先する。

```java
// config/MaintenanceFilter.java
public class MaintenanceFilter extends OncePerRequestFilter {

    private static final String MAINTENANCE_KEY = "maintenance:enabled";

    private final RedisTemplate<String, String> redisTemplate;

    public MaintenanceFilter(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {

        boolean underMaintenance =
                "true".equals(redisTemplate.opsForValue().get(MAINTENANCE_KEY));

        if (underMaintenance && !isMaintenanceAdminEndpoint(request)) {
            response.setStatus(503);
            response.setHeader("Retry-After", "3600");
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("""
                    {"status":503,"error":"Service Unavailable","message":"ただいまメンテナンス中です"}
                    """);
            return; // ✅ chain.doFilter() を呼ばず、ここで処理を終わらせる
        }
        chain.doFilter(request, response);
    }

    private boolean isMaintenanceAdminEndpoint(HttpServletRequest request) {
        // フラグ自体を操作する管理者用エンドポイントは除外する
        return request.getRequestURI().startsWith("/api/admin/maintenance");
    }
}
```

**原則:**

- フラグの保存先は Redis に統一する（DBやファイルでの管理は切り替えの即時性に欠けるため避ける）
- メンテナンス中でもフラグを解除できる管理者用エンドポイントは除外する
- 大規模な計画停止（インフラ自体を止める場合）は Filter ではなく ALB のリスナールール（固定レスポンス）で対応する。Filter はアプリが起動している前提の制御であることに注意する

---

## 13.5 フィルターの登録と実行順序

#### 登録方法の使い分け

| 登録方法 | 実行チェーン | 使うべきケース |
|---|---|---|
| `SecurityConfig.addFilterBefore/After` | Spring Security Filter Chain（内側） | 認証結果・MDC・ロギングなど Security と連携するフィルター |
| `@Component` + `@Order` | Servlet Filter Chain（外側） | Spring Security と無関係なフィルター（CORS 等） |
| `FilterRegistrationBean` | Servlet Filter Chain（外側・URL限定可） | 特定パスのみに適用するフィルター（静的リソース等） |

`SecurityConfig.addFilterBefore/After` で登録したフィルターは Spring Security Filter Chain の内側で動作するため、`SecurityContextHolder` の認証情報を参照できる。`@Component` は使わない。**`@Component` でFilterを登録しない**（Spring Boot が Servlet Filter として自動登録し、二重実行される）。

```java
// config/SecurityConfig.java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

    // ✅ メンテナンスモードは全フィルターより最優先（他の処理コストをかけずに遮断する）
    http.addFilterBefore(new MaintenanceFilter(redisTemplate), RateLimitingFilter.class);
    // ✅ レート制限は認証フィルターより前（認証コストをかける前に弾く）
    http.addFilterBefore(new RateLimitingFilter(), RequestTracingFilter.class);
    // ✅ MDC 確立を最優先にするため SecurityContextHolderFilter より前に置く
    http.addFilterBefore(new RequestTracingFilter(), SecurityContextHolderFilter.class);
    // ✅ ロギングはトレーシングの後（requestId が MDC にセットされた状態でログを出す）
    http.addFilterAfter(new RequestLoggingFilter(), RequestTracingFilter.class);

    // ... 既存の設定
    return http.build();
}
```

**実行順序:**

```
リクエスト受信
    │
    ▼
MaintenanceFilter         ← メンテナンス中なら 503 を返してここで終了
    │
    ▼
RateLimitingFilter        ← 超過なら 429 を返してここで終了
    │
    ▼
RequestTracingFilter      ← MDC 確立（requestId / userId）
    │
    ▼
Spring Security Filters   ← 認証・認可（401 / 403）
    │
    ▼
RequestLoggingFilter      ← ログ出力（認証後のユーザ情報も含めて記録）
    │
    ▼
DispatcherServlet → Controller → Service
```

**`@Component` で登録してはいけない理由:**

```java
// ❌ @Component を付けると Spring Boot が FilterRegistrationBean を自動生成し、
//    SecurityFilterChain への登録と合わせて「2回」実行される
@Component
public class RequestTracingFilter extends OncePerRequestFilter { ... }

// ✅ SecurityConfig の addFilterBefore / addFilterAfter でのみ登録する
//    OncePerRequestFilter が「1リクエスト1回」を保証する
```

**`FilterRegistrationBean` を使うケース:**

Spring Security の管理外（静的リソース配信等）に適用する場合のみ使用する。

```java
// ✅ Spring Security の FilterChain を経由しない場合のみ
@Bean
public FilterRegistrationBean<SomeFilter> someFilterRegistration() {
    FilterRegistrationBean<SomeFilter> registration = new FilterRegistrationBean<>();
    registration.setFilter(new SomeFilter());
    registration.addUrlPatterns("/static/*");
    registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
    return registration;
}
```
