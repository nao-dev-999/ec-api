# 7. Spring Security

> [← インデックスに戻る](../コーディング規約.md)

---

## SecurityConfig の基本方針

```java
// 認可: hasAnyAuthority() を使う（hasAnyRole()はROLE_プレフィックスが二重になるリスクがある）
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/api/admin/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_PRODUCT_MANAGER")
    .requestMatchers("/api/customer/**").hasAuthority("ROLE_CUSTOMER")
    .requestMatchers("/api/auth/**").permitAll()
    .anyRequest().authenticated()
)
```
authorizeHttpRequests は Spring Security の HttpSecurity クラス（org.springframework.security.config.annotation.web.builders.HttpSecurity）のメソッド。
SecurityFilterChain を構築する際に HttpSecurity インスタンスに対して呼び出し、URLパターンごとの認可ルールを設定します。

hasAnyAuthority、permitAll、authenticatedは AuthorizeHttpRequestsConfigurer.AuthorizedUrl（org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurerの内部クラス）のメソッドです。
requestMatchers("/api/admin/**") の戻り値がこの AuthorizedUrl オブジェクトで、 それに対して .hasAnyAuthority("ROLE_ADMIN", "ROLE_PRODUCT_MANAGER")のように認可条件を指定します。

---

## SecurityFilterChain の実装（SecurityConfig.java）

### HttpSecurity と SecurityFilterChain の関係

- `SecurityFilterChain` は「どのリクエストに適用するか（`matches()`）」と「適用する `Filter` のリスト（`getFilters()`）」を保持するだけのコンテナで、それ自体は `Filter` ではない。
- 実際の認証・認可・CORS・CSRF・例外処理は、そのリストに含まれる個々の `Filter` 実装（`CorsFilter`, `CsrfFilter`, `SecurityContextHolderFilter`, `AuthorizationFilter`, `ExceptionTranslationFilter` など）が担っている。
- `HttpSecurity` はこれらの `Filter` を生成・正しい順序で組み立てるビルダークラス。`.cors()`, `.csrf()`, `.sessionManagement()`, `.authorizeHttpRequests()`, `.exceptionHandling()` などの設定メソッドはそれぞれ対応する `Filter` を追加し、最後に `.build()` を呼ぶことで `SecurityFilterChain` の実装クラス（`DefaultSecurityFilterChain`）を生成する（`SecurityConfig.java:108`）。
- 開発者は `HttpSecurity` に対して、実現したい認証認可・セッション管理・CSRF・CORSの設定を行う。
- 例外ハンドリング（401/403）も `ExceptionTranslationFilter` というチェーン内のFilterが担当するため、`.exceptionHandling()` で `HttpSecurity` に設定する。
- `addFilterBefore` / `addFilterAfter` で、チェーン内の特定Filterの前後に独自Filterを挿入することもできる。

### filterChain(HttpSecurity http) の設定内容

`filterChain(HttpSecurity http)` は `SecurityFilterChain` を返す `@Bean` メソッドで、`HttpSecurity` にメソッドチェーンで各種設定を積み上げている。

1. **CORS設定**: `cors(...)` で `corsConfigurationSource()` を使用。`app.cors.allowed-origins`（デフォルト `http://localhost:3000`）からの GET/POST/PUT/DELETE/PATCH/OPTIONS を許可し、資格情報（Cookie等）も許可。
2. **CSRF無効化**: `csrf(AbstractHttpConfigurer::disable)` — REST API のため CSRF 保護はオフ。
3. **セッション管理**: `sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)` — 必要な場合のみセッションを作成。
4. **認可ルール**（上から順にマッチしたルールが適用される）
   - `/swagger-ui/**`, `/v3/api-docs/**` → 認証不要
   - `/api/auth/**` → 認証不要（管理者ログイン等）
   - `/api/customer/products/**` → 認証不要（商品参照は誰でも可）
   - `/api/customer/auth/**` → 認証不要（顧客ログイン等）
   - `/api/customer/cart/**`, `/api/customer/me/**`, `/api/orders/**` → `CUSTOMER` ロール必須
   - `/api/admin/**` → `ADMIN` または `PRODUCT_MANAGER` ロール必須
   - それ以外の全リクエスト → `denyAll()`（明示的に許可されていないパスは全拒否）
5. **例外ハンドリング**（`ExceptionTranslationFilter` が後続Filterの例外を捕捉して委譲）
   - `authenticationEntryPoint`: 未認証アクセス時（401 Unauthorized）に `messageHelper` から i18n メッセージを取得し JSON で返却
   - `accessDeniedHandler`: 認可エラー時（403 Forbidden）に同様に JSON で返却
6. **カスタムフィルタ追加**（`addFilterBefore` / `addFilterAfter` によるチェーン内の前後挿入）
   - `RequestTracingFilter` を `SecurityContextHolderFilter` の前に挿入
   - `RequestLoggingFilter` を `RequestTracingFilter` の後に挿入

補足:
- クラスレベルの `@EnableMethodSecurity` により、`@PreAuthorize` 等のメソッドレベル認可も併用可能。
- 管理者用（`authenticationManager`）と顧客用（`customerAuthenticationManager`）で認証プロバイダを分けており、それぞれ別の `UserDetailsService` 実装（`UserDetailsServiceImpl` / `CustomerUserDetailsService`）を使用する。

### CorsConfigurationSource（CORS設定の詳細）

`org.springframework.web.cors.CorsConfigurationSource` は、「リクエストに応じてどの `CorsConfiguration` を適用するか」を決定するインターフェース。

```java
public interface CorsConfigurationSource {
    CorsConfiguration getCorsConfiguration(HttpServletRequest request);
}
```

`CorsFilter`（`.cors()` で組み込まれるチェーン内Filter）が各リクエストに対してこのメソッドを呼び出し、返ってきた `CorsConfiguration` に基づいて Origin・Method・Header の許可判定や `Access-Control-*` レスポンスヘッダーの付与を行う。

```java
private CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(allowedOrigins);
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
    configuration.setAllowedHeaders(List.of("*"));
    configuration.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
}
```

- **`CorsConfiguration`**: 実際の許可ルールを保持するデータクラス
  - `allowedOrigins`: 許可オリジン（`app.cors.allowed-origins` プロパティ、デフォルト `http://localhost:3000`）
  - `allowedMethods`: 許可HTTPメソッド
  - `allowedHeaders`: `"*"` で全ヘッダー許可
  - `allowCredentials(true)`: Cookie等の資格情報付きリクエストを許可（フロントとセッションCookieをやり取りするため必須）
- **`UrlBasedCorsConfigurationSource`**: `CorsConfigurationSource` の実装クラス。URLパターン（`AntPathMatcher`）ごとに異なる `CorsConfiguration` を登録可能。`registerCorsConfiguration("/**", configuration)` で全パスに同一設定を適用している。
- この `corsConfigurationSource()` は `.cors(cors -> cors.configurationSource(corsConfigurationSource()))`（SecurityConfig.java:45）で `HttpSecurity` に渡され、`SecurityFilterChain` 内の `CorsFilter` に設定される。

補足:
- `allowCredentials(true)` を使う場合、`allowedOrigins` に `"*"` は指定できない（仕様上禁止）。本プロジェクトでは具体的なオリジンリストを渡しているため問題ない。
- パスごとに異なるCORS設定が必要な場合は、`registerCorsConfiguration` を複数回呼んで細かく制御することも可能。

### @EnableWebSecurity と Spring Boot 自動設定の関係

`spring-boot-security` の `ServletWebSecurityAutoConfiguration`（Spring Boot 4.0.1 のソースで確認）は、独立した2つの自動設定を持つ。

1. **デフォルトの `SecurityFilterChain`**（`@ConditionalOnDefaultWebSecurity`）
   - `spring-boot-starter-security` を入れただけの状態で有効になる「全リクエスト認証必須 + フォームログイン + HTTP Basic」の Bean。
   - **自分で `SecurityFilterChain` の `@Bean` を1つでも定義すると完全にバックオフ**する。本プロジェクトは `SecurityConfig.filterChain()` を定義しているため、こちらは動作していない。
2. **`@EnableWebSecurity` の自動付与**（`@ConditionalOnMissingBean(name = BeanIds.SPRING_SECURITY_FILTER_CHAIN)`）
   - `springSecurityFilterChain`（`@EnableWebSecurity` が内部で作る `FilterChainProxy` Bean）がまだ存在しない場合、Boot が代わりに `@EnableWebSecurity` を自動適用する。
   - これは①とは独立した条件のため、**自前の `SecurityFilterChain` Bean の有無に関わらず**動作する。

**本プロジェクトの `SecurityConfig` は `@EnableWebSecurity` を書いていない**（明示的に削除済み）。つまり②の自動付与が実際に発動しており、`@EnableWebSecurity` の効果はこの仕組み経由で得ている。

#### `@SpringBootApplication` から ② が発動するまでの流れ

②がなぜ発動するのか、`@SpringBootApplication`（`EcApiApplication`）を起点に実ソースで追った流れ。

```
@SpringBootApplication（EcApiApplication）
  = @SpringBootConfiguration + @EnableAutoConfiguration + @ComponentScan

@EnableAutoConfiguration
  = @Import(AutoConfigurationImportSelector.class)

AutoConfigurationImportSelector.getCandidateConfigurations()
  → ImportCandidates.load(AutoConfiguration.class, classLoader)
      → location = "META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports"
      → classLoader.getResources(location) でクラスパス上の全jarから同名ファイルを収集
          → spring-boot-security-4.0.1.jar 内にも同名ファイルがあり、
            ServletWebSecurityAutoConfiguration 等9クラスが列挙されている
      → 収集した全クラス名を候補として返す

ServletWebSecurityAutoConfiguration
  @ConditionalOnClass(EnableWebSecurity.class)      ← spring-boot-starter-security で満たす
  @ConditionalOnWebApplication(type = SERVLET)      ← spring-boot-starter-webmvc で満たす
  → 候補が条件を満たし有効化

  └ EnableWebSecurityConfiguration
      @ConditionalOnMissingBean(name = BeanIds.SPRING_SECURITY_FILTER_CHAIN)
      @EnableWebSecurity
      → springSecurityFilterChain Bean がまだ無いため条件成立 → @EnableWebSecurity が自動適用される
```

`classLoader.getResources(...)` は単数形の `getResource` ではなく複数形なので、クラスパス上の**全jar**から同名ファイルを集約する点がポイント。これにより `spring-boot-autoconfigure` と `spring-boot-security` など複数モジュールの `*.imports` ファイルが1つのリストにまとまり、依存追加だけで自動設定が増える仕組みが成立している。

---

## メソッドセキュリティ

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

---

## セッション管理（Redis）

- JWT は使用しない（`JwtAuthenticationFilter`, `JwtHelper`, `JwtProperties`, `TokenRedisService` は削除済み）
- Spring Session + Redis でセッション管理
- CSRF は REST API（`Content-Type: application/json` + CORS制御）では無効化可
- Cookie は `SameSite=Lax` で CSRF 保護


