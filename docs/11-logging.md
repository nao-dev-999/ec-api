# 11. ログ設計

> **方針:** 本番運用を想定した JSON 構造化ログ。リクエスト単位でトレース可能な設計とし、
> 機密情報は絶対にログに出力しない。

---

## 11.1 ログカテゴリ

本プロジェクトのログは以下の3カテゴリで考える。
**「誰が、何のために出すか」** を明確にし、出力責務を分散させる。

| カテゴリ | 目的 | 出力者 | レベル |
|----------|------|--------|--------|
| **アクセスログ** | 全 HTTP リクエスト・レスポンスの記録。SLA 監視・障害のタイムライン把握に使う | `RequestLoggingFilter`（自動） | `INFO` |
| **アプリログ** | ビジネス的に意味のある処理の記録。サービスメソッドの開始・終了、外部 API 呼び出し等 | `ServiceLoggingAspect`（自動）+ 開発者（手動） | `INFO` / `DEBUG` |
| **エラーログ** | 異常系の記録。ビジネス例外（WARN）と予期しないサーバーエラー（ERROR） | `GlobalExceptionHandler`（自動） | `WARN` / `ERROR` |

### カテゴリと出力者のマッピング

```
HTTP リクエスト受信
    │
    ▼
[RequestLoggingFilter]  ← アクセスログ（method, path, status, responseTimeMs）
    │  MDC.put("requestId", ...)
    ▼
[Spring Security]
    │
    ▼
[Controller]            ← 原則ログ出力なし（アクセスログで代替できる）
    │
    ▼
[ServiceLoggingAspect]  ← アプリログ（method, responseTimeMs） ※ DEBUG
    │
    ▼
[Service（手動）]       ← アプリログ（ビジネスイベント） ※ INFO
    │  ├─ 外部 API 呼び出し前後 → アプリログ（url, status, responseTimeMs）
    │  └─ 重要な状態遷移       → アプリログ（orderId, status）
    ▼
[GlobalExceptionHandler] ← エラーログ（WARN: ビジネス例外 / ERROR: 予期しない例外）
    │
    ▼
HTTP レスポンス送信
    │
    ▼
[RequestLoggingFilter]  ← アクセスログ完了（status, responseTimeMs を付加）
    │  MDC.clear()
```

---

## 11.2 ログレベルの使い分け

| レベル | 用途 | 出力例 |
|--------|------|--------|
| `ERROR` | 即時対応が必要な障害。予期しない例外、外部 API のタイムアウト | `handleGeneral` での 500、外部 API の接続不可 |
| `WARN` | 正常系ではないが運用を止めない事象 | ビジネス例外（404, 409）、バリデーションエラー、外部 API の 4xx |
| `INFO` | 運用監視に必要な記録。本番で常時出力される | リクエスト完了、注文確定、ステータス遷移 |
| `DEBUG` | 開発時のデバッグ情報。**本番では出力しない** | サービスメソッドの処理時間、SQL クエリ |

**原則:**

- `ERROR` ログには必ず例外オブジェクトを渡す（スタックトレースを残す）: `log.error("msg", ex)`
- `WARN` ログにスタックトレースは不要（メッセージのみ）
- `log.debug()` の評価コストが高い場合はラムダで遅延評価する: `log.debug(() -> "val=" + heavyOp())`
- オブジェクト全体を `toString()` で渡さない。必要なフィールドを個別に指定する

---

## 11.3 MDC によるリクエスト ID 伝播

### 設計

`requestId`（UUID）をリクエストの入口でセットし、同一リクエスト内の全ログに自動付与する。
MDC（Mapped Diagnostic Context）は SLF4J の機能でスレッドローカルに値を保持する。
JSON ログではこの値が全ログエントリに含まれるため、リクエスト単位でのログ抽出が可能になる。

```
MDC.put("requestId", "a3f2c1d4-...")  ← RequestLoggingFilter でセット
    │
    ├─ INFO  requestId=a3f2c1d4  "Request started"
    ├─ DEBUG requestId=a3f2c1d4  "OrderService.createOrder responseTimeMs=43"
    ├─ INFO  requestId=a3f2c1d4  "Order confirmed orderId=99"
    └─ INFO  requestId=a3f2c1d4  "Request completed status=201 responseTimeMs=87"

MDC.clear()  ← RequestLoggingFilter の finally でクリア
```

**原則:**

- `MDC.clear()` は必ず `finally` ブロックで実行する（スレッドプール再利用時の汚染防止）
- ログインユーザーの ID は Security フィルター通過後に `MDC.put("userId", ...)` でセットする
  （未認証リクエストは `null` になるため、セット箇所は Security フィルターより後）

---

## 11.4 レイヤー別のログ出力責務

### 11.4.1 アクセスログ（RequestLoggingFilter）

全 HTTP リクエスト・レスポンスを記録する。`HandlerInterceptor` ではなく
`OncePerRequestFilter` を使う理由は、Spring Security の 401/403 など
DispatcherServlet に到達しないレスポンスもカバーするため。

```java
// config/RequestLoggingFilter.java
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);
    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // API Gateway 等から requestId が来た場合はそれを引き継ぐ。なければ自前で生成する
        String requestId = Optional.ofNullable(request.getHeader(REQUEST_ID_HEADER))
                .filter(s -> !s.isBlank())
                .orElse(UUID.randomUUID().toString());

        MDC.put("requestId", requestId);
        response.setHeader(REQUEST_ID_HEADER, requestId); // クライアントが確認できるよう返す

        long startTime = System.currentTimeMillis();
        try {
            log.info("Request started method={} path={}",
                    request.getMethod(), request.getRequestURI());

            filterChain.doFilter(request, response);

            log.info("Request completed method={} path={} status={} responseTimeMs={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    System.currentTimeMillis() - startTime);

        } finally {
            MDC.clear();
        }
    }
}
```

**出力フィールド:**

| フィールド | 説明 |
|-----------|------|
| `requestId` | リクエスト単位のトレース ID（MDC 経由で全ログに付与） |
| `method` | HTTP メソッド |
| `path` | リクエストパス（クエリストリングは除く） |
| `status` | HTTP レスポンスステータス |
| `responseTimeMs` | リクエスト処理時間（ms） |

> **注意:** クエリストリングはログに含めない（検索キーワード等が個人情報になりうる）。
> `LoggingInterceptor` はこの Filter に役割を移譲するため**削除する**。

---

### 11.4.2 Controller 層

**原則としてログを出力しない。** アクセスログ（RequestLoggingFilter）で
method / path / status / responseTimeMs は記録済みのため、Controller での追加ログは重複になる。

例外として、リクエストボディの内容をデバッグしたい場合は `DEBUG` レベルで手動出力する。
ただし機密情報を含むフィールドは絶対に出力しない（→ 11.6 参照）。

```java
// ✅ デバッグ目的の場合のみ。productId など特定フィールドのみ出力する
log.debug("createProduct requested name={}", request.name());

// ❌ リクエストオブジェクト全体は出さない（パスワード等が含まれる可能性）
log.debug("createProduct request={}", request);
```

---

### 11.4.3 Service 層（AOP による自動ログ）

`ServiceLoggingAspect` が全 Service メソッドの処理時間を **`DEBUG`** レベルで自動記録する。
本番では出力されないため、パフォーマンス影響はない。

```java
// config/ServiceLoggingAspect.java
@Aspect
@Component
public class ServiceLoggingAspect {

    @Pointcut("execution(* com.example.ecapi.service..*.*(..))")
    public void serviceMethods() {}

    @Around("serviceMethods()")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        Logger log = LoggerFactory.getLogger(joinPoint.getTarget().getClass());
        String method = joinPoint.getSignature().toShortString();
        long start = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            log.debug("method={} responseTimeMs={}", method, System.currentTimeMillis() - start);
            return result;
        } catch (Throwable t) {
            // 例外は GlobalExceptionHandler でログを出すため、ここでは二重にしない
            log.debug("method={} threw={}", method, t.getClass().getSimpleName());
            throw t;
        }
    }
}
```

**禁止事項（現状の実装から修正が必要）:**

```java
// ❌ 引数をそのまま出力しない（password 等が平文でログに出る）
log.info("args: {}", Arrays.toString(joinPoint.getArgs()));

// ❌ 戻り値をそのまま出力しない（個人情報を含むレスポンス全体が出る）
log.info("result: {}", result);
```

---

### 11.4.4 Service 層（手動ログ）

AOP の自動ログ（DEBUG）とは別に、**ビジネス的に意味のある処理** は開発者が手動で `INFO` ログを出す。

**手動ログを出すべきイベントの基準:**

- リソースの作成・更新・削除（ID が確定したタイミング）
- 注文・決済など金銭が絡む状態遷移
- ユーザーの認証・ログアウト

```java
// ✅ ビジネスイベントのログ（何が起きたかを ID で記録する）
@Transactional
public OrderResult createOrder(CreateOrder command) {
    // ... 注文処理 ...
    var saved = orderRepository.save(order);
    log.info("Order created orderId={} customerId={} totalAmount={}",
            saved.getId(), command.customerId(), saved.getTotalAmount());
    return mapper.toOrderResult(saved);
}

// ✅ 状態遷移のログ
public void cancelOrder(Long orderId) {
    // ...
    log.info("Order cancelled orderId={}", orderId);
}
```

**出力フィールドの基準:**

- リソースの ID（`orderId`, `productId` 等）は必ず含める
- 金額・数量など監査に必要な値は含める
- 個人情報（名前・メールアドレス等）は含めない

---

### 11.4.5 外部 API 呼び出し

外部 API（決済サービス・通知サービス・社内マイクロサービス等）の呼び出しは
**必ず前後にログを出す**。外部依存は障害の起点になりやすく、
タイムアウトや一時エラーの記録がないと原因調査が困難になる。

```java
// ✅ 外部 API 呼び出しのログパターン
@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    public PaymentResult charge(Long orderId, int amount) {
        log.info("External API call started service=payment-api action=charge orderId={}", orderId);
        long start = System.currentTimeMillis();
        try {
            PaymentResult result = paymentApiClient.charge(orderId, amount);
            log.info("External API call completed service=payment-api action=charge"
                    + " orderId={} status={} responseTimeMs={}",
                    orderId, result.status(), System.currentTimeMillis() - start);
            return result;
        } catch (PaymentApiException ex) {
            // 外部 API の 4xx 等、呼び出しは成功しているがビジネス的に失敗した場合
            log.warn("External API call failed service=payment-api action=charge"
                    + " orderId={} status={} responseTimeMs={}",
                    orderId, ex.getStatus(), System.currentTimeMillis() - start);
            throw new PaymentFailedException(messageHelper.get("error.payment.failed"));
        } catch (Exception ex) {
            // タイムアウト・接続不可など外部サービス自体の障害
            log.error("External API call error service=payment-api action=charge"
                    + " orderId={} responseTimeMs={}",
                    orderId, System.currentTimeMillis() - start, ex);
            throw new ExternalServiceException(messageHelper.get("error.external.unavailable"));
        }
    }
}
```

**出力フィールドの基準:**

| フィールド | 説明 |
|-----------|------|
| `service` | 呼び出し先サービス名（固定値） |
| `action` | 実行した操作 |
| リソース ID | `orderId` など、追跡に必要な ID |
| `status` | 外部 API のレスポンスステータス（取得できる場合） |
| `responseTimeMs` | 外部 API のレスポンスタイム |

**ログレベルの判断基準:**

- 正常完了 → `INFO`
- 外部 API の 4xx（呼び出しは成功、ビジネス的に失敗）→ `WARN`
- タイムアウト・接続不可・5xx → `ERROR`（スタックトレースを含める）

---

### 11.4.6 エラーログ（GlobalExceptionHandler）

エラーログは `GlobalExceptionHandler` が一元的に出力する。
個々の Service・Controller でエラーログを出すと二重ログになるため禁止。

```java
// ✅ ビジネス例外は WARN（スタックトレース不要）
@ExceptionHandler(BusinessException.class)
public ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex) {
    log.warn("{}: {}", ex.getClass().getSimpleName(), ex.getMessage());
    return buildError(ex.getStatus(), ex.getMessage());
}

// ✅ 予期しない例外は ERROR（スタックトレース必須）
@ExceptionHandler(Exception.class)
public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
    log.error("Unexpected exception occurred", ex);
    return buildError(HttpStatus.INTERNAL_SERVER_ERROR, messageHelper.get("error.system"));
}
```

**禁止事項:**

```java
// ❌ Service 内で例外をキャッチして ERROR ログを出した後にre-throw
// → GlobalExceptionHandler でも出るため二重ログになる
try {
    orderRepository.save(order);
} catch (Exception e) {
    log.error("Failed to save order", e); // ❌
    throw e;
}
```

---

## 11.5 JSON 構造化ログの設定

### logback-spring.xml

`src/main/resources/logback-spring.xml` に配置する。
`logback-spring.xml`（Spring 拡張）を使うことで `<springProfile>` による環境別切り替えが可能。

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>

    <!-- ローカル・テスト: 可読性重視のコンソール出力 -->
    <springProfile name="local,test">
        <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
            <encoder>
                <pattern>%d{HH:mm:ss.SSS} [%level] [%X{requestId}] %logger{36} - %msg%n</pattern>
            </encoder>
        </appender>
        <root level="INFO">
            <appender-ref ref="CONSOLE"/>
        </root>
    </springProfile>

    <!-- 本番・ステージング: JSON 構造化ログ -->
    <springProfile name="prod,staging">
        <appender name="JSON_CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
            <encoder class="net.logstash.logback.encoder.LogstashEncoder">
                <!-- MDC フィールド（requestId, userId 等）は自動的に JSON に含まれる -->
                <includeContext>false</includeContext>
                <timestampPattern>yyyy-MM-dd'T'HH:mm:ss.SSS'Z'</timestampPattern>
            </encoder>
        </appender>
        <root level="INFO">
            <appender-ref ref="JSON_CONSOLE"/>
        </root>
        <!-- 本番では SQL ログを出力しない -->
        <logger name="org.hibernate.SQL" level="OFF"/>
        <logger name="org.hibernate.orm.jdbc.bind" level="OFF"/>
    </springProfile>

</configuration>
```

**`build.gradle.kts` への依存追加:**

```kotlin
dependencies {
    implementation("net.logstash.logback:logstash-logback-encoder:8.0")
}
```

### application.yml のログ設定

```yaml
# application.yml（デフォルト。本番・ステージング兼用）
logging:
  level:
    root: WARN
    com.example.ecapi: INFO

# application-local.yml（ローカル開発用上書き）
logging:
  level:
    root: WARN
    com.example.ecapi: DEBUG
    org.hibernate.SQL: DEBUG
    org.hibernate.orm.jdbc.bind: TRACE
```

**原則:**

- `show-sql: true` は**本番・ステージングでは使用しない**
  （標準出力への平文 SQL は JSON ログに乗らず、検索・集計ができない）
- SQL ログが必要な環境では `org.hibernate.SQL: DEBUG` を `application-local.yml` に限定する
- `root: WARN` をデフォルトとし、Spring Framework / Hibernate 内部ログを抑制する

---

## 11.6 機密情報のマスキング規則

以下のフィールドはログに**絶対に出力しない**。

| カテゴリ | 対象フィールド |
|----------|---------------|
| 認証情報 | `password`, `rawPassword`, `token`, `sessionId` |
| 個人情報 | `email`, `phoneNumber`, `address` |
| 決済情報 | `creditCardNumber`, `cvv` |
| HTTP ヘッダー | `Authorization` ヘッダーの値、`Cookie` ヘッダーの値 |

**チェックポイント（PR レビュー時に確認）:**

- DTO / Entity を `toString()` でログに渡していないか
- `joinPoint.getArgs()` を直接出力していないか
- リクエストボディを文字列変換してログに出していないか
- クエリストリングをそのままログに出していないか

---

## 11.7 ログ設計チェックリスト

- [ ] アクセスログ: `RequestLoggingFilter` が `method` / `path` / `status` / `responseTimeMs` を出している
- [ ] アプリログ: ビジネスイベント（リソース作成・状態遷移）に `INFO` ログがある
- [ ] 外部 API: 呼び出し前後に `INFO`、失敗時に `WARN` / `ERROR` ログがある
- [ ] エラーログ: ビジネス例外は `WARN`、予期しない例外は `ERROR`（スタックトレース付き）になっている
- [ ] 二重ログなし: Service 内でキャッチして再 throw する際に `log.error()` を呼んでいない
- [ ] 機密情報なし: パスワード・メールアドレス・トークンがログに含まれていない
- [ ] MDC: `MDC.clear()` が `finally` ブロックに入っている
- [ ] 本番設定: `show-sql: true` が本番プロファイルに残っていない
- [ ] `ERROR` ログには例外オブジェクト（スタックトレース）が渡されている
