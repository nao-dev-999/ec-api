# 18. セキュリティヘッダー・入力サイズ制限・ログマスキング

> [← インデックスに戻る](../コーディング規約.md)

---

[7. Spring Security](07-spring-security.md) は認証・認可・CORS・セッションを扱うが、本章ではそこに含まれない「レスポンスヘッダー」「入力サイズ」「ログ出力」のセキュリティ強化方針を扱う。

## 1. セキュリティヘッダー

Spring Securityはデフォルトで `X-Content-Type-Options: nosniff` 等を付与するが、本プロジェクトの `SecurityConfig` では明示設定をしていない。デフォルト任せにせず、意図を明文化した上で `HttpSecurity.headers()` に明示する。

```java
// config/SecurityConfig.java
http.headers(headers -> headers
    .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'"))
    .referrerPolicy(referrer -> referrer.policy(ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
    .httpStrictTransportSecurity(hsts -> hsts
            .includeSubDomains(true)
            .maxAgeInSeconds(31536000))
    .frameOptions(FrameOptionsConfig::deny)
);
```

| ヘッダー | 値 | 目的 |
|---|---|---|
| `X-Content-Type-Options` | `nosniff`（Spring Securityデフォルト） | MIMEタイプスニッフィングによるXSS防止 |
| `X-Frame-Options` | `DENY` | クリックジャッキング対策（管理画面・顧客画面ともiframe埋め込み不要） |
| `Strict-Transport-Security` | `max-age=31536000; includeSubDomains` | HTTPSの強制（ALBがHTTPS終端のため、アプリ側でも明示する） |
| `Content-Security-Policy` | `default-src 'self'` を起点にフロントエンドの構成に応じて調整 | XSS被害の影響範囲を限定する |
| `Referrer-Policy` | `strict-origin-when-cross-origin` | 他サイトへの遷移時にURL内の個人情報・トークンが漏れないようにする |

APIレスポンス（JSON）が主体のためCSPの実効性はフロントエンド（Next.js）側の設定と合わせて検討する。本項目はAPIサーバ側の最低限の設定を定めるものであり、フロントエンドのCSP設計は別途フロントエンド側の規約に委ねる。

## 2. 入力サイズ制限

DoS対策・リソース枯渇対策として、以下の上限を明文化する。

```yaml
# application.yml
server:
  tomcat:
    max-http-form-post-size: 2MB   # application/x-www-form-urlencoded, multipart以外のフォーム
  max-http-request-header-size: 16KB

spring:
  servlet:
    multipart:
      max-file-size: 5MB
      max-request-size: 5MB
```

| 対象 | 上限の目安 | 備考 |
|---|---|---|
| リクエストボディ全体 | 1MB（JSON API） | 商品画像等は[17. ファイルアップロード](17-file-upload.md)の署名付きURL方式を使うためAPI本体では大きなボディを受けない |
| 文字列フィールド（DTO） | `@Size(max = ...)` を**すべての `String` フィールドに付与する** | 上限なしの `@NotBlank` のみは禁止。DB列長（[9. データベース](09-database-flyway.md)のVARCHAR長）と必ず一致させる |
| 配列・リストフィールド | `@Size(max = ...)` を付与する | 検索条件のIDリスト等、上限のない配列はDB負荷・レスポンスサイズの両面でリスクになる |
| ページサイズ | 既存の `MAX_PAGE_SIZE = 100` 方針を全一覧エンドポイントで統一する | [8. ページネーション](../API_INTERFACE_CONVENTIONS.md#8-ページネーション)参照 |

```java
// ✅ 上限のある配列
public record BulkDisableRequest(
    @NotEmpty @Size(max = 100) List<Long> productIds
) {}

// ❌ 上限のない配列(数万件送られるとDBクエリが肥大化する)
public record BulkDisableRequest(
    @NotEmpty List<Long> productIds
) {}
```

## 3. ログ内の機微情報マスキング

[12. ロギング](12-logging.md)でリクエスト/レスポンスボディを出力しない方針は定義済みだが、**URL・クエリパラメータ・例外メッセージ**に機微情報が混入するケースは別途対策が必要。

```java
// ❌ クエリパラメータをそのままログに出す(メールアドレス等が残る)
log.info("search request: {}", request.getQueryString());

// ✅ ログに出してよいキーをホワイトリスト化し、それ以外はマスクする
log.info("search request: page={}, size={}", page, size); // センシティブでない値のみ個別に出力
```

| 対象 | 方針 |
|---|---|
| アクセスログ(`RequestLoggingFilter`) | メソッド・パス・ステータス・処理時間のみ出力（実装済み）。**クエリパラメータは出力対象に含めない** |
| 例外ログ | `BusinessException` の `args`（メッセージ埋め込み値）に個人情報を含める場合、ログ出力時はマスクする。例: メールアドレスは `u***@example.com` 形式にする |
| 監査ログ(`AuditorAware`) | `created_by`/`updated_by` はユーザIDのみを記録し、氏名・メールアドレス等は記録しない |
| スタックトレース | 本番ログには出力してよい（内部確認用）が、**レスポンスには絶対に含めない**（[6. エラーレスポンス](../API_INTERFACE_CONVENTIONS.md#6-エラーレスポンス)で定義済み） |

マスキング関数は `helper` パッケージに共通化し、各所で個別実装しない。

```java
// helper/MaskingHelper.java
public final class MaskingHelper {
    public static String maskEmail(String email) {
        int at = email.indexOf('@');
        if (at <= 1) return "***" + email.substring(at);
        return email.charAt(0) + "***" + email.substring(at);
    }
}
```

---

## チェックリスト

- [ ] `SecurityConfig` にセキュリティヘッダー(`frameOptions`/`hsts`/`contentSecurityPolicy`等)を明示している
- [ ] リクエストボディ・multipartのサイズ上限を `application.yml` に設定している
- [ ] DTOの `String` フィールド・配列フィールドに `@Size(max = ...)` を付与している
- [ ] クエリパラメータ・例外メッセージの個人情報をログ出力時にマスクしている
