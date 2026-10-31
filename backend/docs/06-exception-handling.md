# 6. 例外処理

> [← インデックスに戻る](../コーディング規約.md)

---

## 6.1 例外クラス階層

全てのビジネス例外は `BusinessException` を頂点とする階層で管理する。
各例外は `ErrorCode`（メッセージキーと HTTP ステータスの組）を保持するため、`GlobalExceptionHandler` は型ごとにハンドラーを増やさず1本で処理できる。

```
BusinessException (abstract)               ← 全ビジネス例外の親。ErrorCode と メッセージ引数を保持
  ├── BadRequestException (abstract)       ← 400 固定
  │     ├── InvalidCurrentPasswordException
  │     └── ProductIdMismatchException
  ├── ResourceNotFoundException (abstract) ← 404 固定
  │     ├── ProductNotFoundException
  │     └── OrderNotFoundException ...
  └── ConflictException (abstract)         ← 409 固定
        ├── InsufficientStockException
        └── CategoryNameDuplicateException ...
```

### ErrorCode

エラーの識別子・メッセージキー・HTTP ステータスを1か所で管理する enum。
`name()` はレスポンスの `code` 項目としてクライアントに返る。

```java
// exception/ErrorCode.java
public enum ErrorCode {
    // 404 Not Found
    PRODUCT_NOT_FOUND("product.notFound", HttpStatus.NOT_FOUND),
    // 409 Conflict
    INSUFFICIENT_STOCK("order.insufficientStock", HttpStatus.CONFLICT),
    OPTIMISTIC_LOCK_CONFLICT("error.conflict.optimisticLock", HttpStatus.CONFLICT),
    // 401 / 403（Spring Security 用）
    UNAUTHORIZED("error.unauthorized", HttpStatus.UNAUTHORIZED),
    FORBIDDEN("error.forbidden", HttpStatus.FORBIDDEN),
    // ...

    private final String messageKey;
    private final HttpStatus status;
    // コンストラクタ・getter 省略
}
```

### BusinessException（基底クラス）

```java
// exception/BusinessException.java
@Getter
public abstract class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;
    private final transient Object[] args;

    protected BusinessException(ErrorCode errorCode, Object... args) {
        super(errorCode.getMessageKey());
        this.errorCode = errorCode;
        this.args = args == null ? new Object[0] : args;
    }

    public HttpStatus getStatus() {
        return errorCode.getStatus();
    }

    /** 中間抽象クラスが担当外ステータスの ErrorCode を受け取っていないか検査する */
    protected static ErrorCode requireStatus(ErrorCode errorCode, HttpStatus expected) {
        if (errorCode.getStatus() != expected) {
            throw new IllegalArgumentException(/* ... */);
        }
        return errorCode;
    }
}
```

### 中間抽象クラス

HTTP ステータスをここで固定する。`requireStatus` により、担当外のステータスを持つ `ErrorCode` を渡すと生成時に失敗する。

```java
// exception/ResourceNotFoundException.java
public abstract class ResourceNotFoundException extends BusinessException {
    protected ResourceNotFoundException(ErrorCode errorCode, Object... args) {
        super(requireStatus(errorCode, HttpStatus.NOT_FOUND), args);
    }
}

// BadRequestException（400）・ConflictException（409）も同じ形
```

### 具象例外クラス

自分の `ErrorCode` を中間クラスに渡すだけ。メッセージの引数（ID など）は可変長引数で受け取る。

```java
// exception/ProductNotFoundException.java
public class ProductNotFoundException extends ResourceNotFoundException {
    public ProductNotFoundException(Object... args) {
        super(ErrorCode.PRODUCT_NOT_FOUND, args);
    }
}

// 呼び出し側（サービス層）
productRepository.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
```

**新しいビジネス例外を追加するときのルール:**

1. `ErrorCode` に定数を追加し、`messages.properties` にメッセージキーを追加する
2. `ErrorCode` のステータスに対応する中間抽象クラス（`BadRequestException` / `ResourceNotFoundException` / `ConflictException`）を継承する。`BusinessException` を直接継承しない
3. どれにも当てはまらない HTTP ステータスが必要な場合は、同じパターンで新たな中間抽象クラスを追加し、`BusinessExceptionHierarchyTest` の対応表にも追加する
4. 具象クラスは「引数なし」または「`Object... args`」のコンストラクタ1つのみ持つ

上記 2〜4 は `BusinessExceptionHierarchyTest` が `exception` パッケージをスキャンして自動検査する。

---

## 6.2 ErrorResponse

```java
// exception/ErrorResponse.java
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String code,
        String message,
        Map<String, String> details) {

    /** ErrorCode からエラーレスポンスを組み立てる */
    public static ErrorResponse of(ErrorCode code, String message, Map<String, String> details) {
        return new ErrorResponse(
                Instant.now(),
                code.getStatus().value(),
                code.getStatus().getReasonPhrase(),
                code.name(),
                message,
                details);
    }
}
```

- `timestamp` は `Instant`（UTC）を使用する。`LocalDateTime` は使わない
- `code` は `ErrorCode.name()`。クライアントはメッセージ文言ではなくこの値で分岐する
- `details` はバリデーションエラー時のみ使用し、それ以外は `null`
- エラーレスポンスは必ず `ErrorResponse.of(...)` で組み立てる。JSON 文字列を手書きしない

---

## 6.3 GlobalExceptionHandler

| ハンドラー | 対象例外 | ステータス / ErrorCode |
|---|---|---|
| `handleBusiness` | `BusinessException`（全サブクラス） | 各 `ErrorCode` |
| `handleOptimisticLock` | `OptimisticLockingFailureException`（Spring）, `jakarta.persistence.OptimisticLockException` | 409 / `OPTIMISTIC_LOCK_CONFLICT` |
| `handleValidation` | `MethodArgumentNotValidException` | 400 / `VALIDATION_ERROR`（`details` 付き） |
| `handleAuthentication` | `AuthenticationException`（ログイン API 内での認証失敗） | 401 / `AUTHENTICATION_FAILED` |
| `handleMessageNotReadable` | `HttpMessageNotReadableException`（JSON パース失敗・不正な enum 値） | 400 / `BAD_REQUEST` |
| `handleTypeMismatch` | `MethodArgumentTypeMismatchException`（パスパラメータの型不正） | 400 / `BAD_REQUEST` |
| `handleGeneral` | `Exception` | 500 / `SYSTEM_ERROR` |

```java
// exception/GlobalExceptionHandler.java（抜粋）
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final MessageHelper messageHelper;

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex) {
        log.warn("{}: code={}", ex.getClass().getSimpleName(), ex.getErrorCode());
        String message = messageHelper.get(ex.getErrorCode().getMessageKey(), ex.getArgs());
        return buildError(ex.getErrorCode(), message);
    }

    @ExceptionHandler({
        OptimisticLockingFailureException.class,
        jakarta.persistence.OptimisticLockException.class
    })
    public ResponseEntity<ErrorResponse> handleOptimisticLock(Exception ex) {
        log.warn("{}: {}", ex.getClass().getSimpleName(), ex.getMessage());
        return buildError(
                ErrorCode.OPTIMISTIC_LOCK_CONFLICT,
                messageHelper.get(ErrorCode.OPTIMISTIC_LOCK_CONFLICT.getMessageKey()));
    }

    // ... 他のハンドラーは上表のとおり

    private ResponseEntity<ErrorResponse> buildError(
            ErrorCode code, String message, Map<String, String> details) {
        return ResponseEntity.status(code.getStatus())
                .body(ErrorResponse.of(code, message, details));
    }
}
```

### 楽観ロック例外の注意点

`@Transactional` のコミット時やリポジトリ呼び出し時に発生した楽観ロック例外は、Spring が
`ObjectOptimisticLockingFailureException`（`OptimisticLockingFailureException` のサブクラス）に変換してからスローする。
JPA の `jakarta.persistence.OptimisticLockException` は `cause` に入るだけで、最上位の型にはならない。

Spring MVC は**最上位の例外型で先にハンドラーを照合し、一致すれば `cause` はたどらない**。
そのため `jakarta.persistence.OptimisticLockException` だけを対象にすると、`Exception` ハンドラーに一致して 500 になる。
楽観ロックのハンドラーには必ず Spring の `OptimisticLockingFailureException` を含めること。

Controller テストで楽観ロックを検証するときも、実際に届く `ObjectOptimisticLockingFailureException` をモックからスローする。

---

## 6.4 Spring Security の 401 / 403

フィルターで弾かれた未認証アクセス（401）・権限不足（403）は DispatcherServlet に届かないため、`@RestControllerAdvice` では処理できない。
`SecurityConfig` の `exceptionHandling()` に `SecurityErrorResponseWriter` を登録する。

```java
// config/SecurityConfig.java（抜粋）
var securityErrorResponseWriter = new SecurityErrorResponseWriter(messageHelper, jsonMapper);
http.exceptionHandling(
        ex ->
                ex.authenticationEntryPoint(securityErrorResponseWriter)
                        .accessDeniedHandler(securityErrorResponseWriter));
```

`SecurityErrorResponseWriter` は `AuthenticationEntryPoint` と `AccessDeniedHandler` を実装し、
`ErrorResponse.of(ErrorCode.UNAUTHORIZED / FORBIDDEN, ...)` を `JsonMapper` でシリアライズして返す。
これにより 401/403 も `GlobalExceptionHandler` と同じ形式（`timestamp`・`code` を含む）になる。

ログイン API 内で `AuthenticationManager` が投げる `AuthenticationException` はコントローラーから送出されるため、`GlobalExceptionHandler.handleAuthentication` が処理する。

---

## 6.5 原則まとめ

- **ビジネス例外は必ず中間抽象クラスを継承する。** `RuntimeException` も `BusinessException` も直接継承しない
- **HTTP ステータスは `ErrorCode` に定義し、中間抽象クラスで検査する。** 具象クラスにも `GlobalExceptionHandler` にもステータスを書かない
- **`GlobalExceptionHandler` に `BusinessException` のサブクラス個別のハンドラーを追加しない。** 新しい例外クラスを追加しても `handleBusiness` が自動的に処理する
- **フレームワーク由来の例外は階層に含めず、`GlobalExceptionHandler` の個別ハンドラーで `ErrorCode` に変換する。** Spring が例外を変換する場合（楽観ロックなど）は変換後の型を対象にする
- **Spring Security の 401/403 は `SecurityErrorResponseWriter` で返す。** `SecurityConfig` に JSON を手書きしない
- **500 は詳細を隠してログに残す。** `handleGeneral` では `ex.getMessage()` をレスポンスに含めない
- **例外メッセージは `messages.properties` で管理する。** 例外には `ErrorCode`（メッセージキー）と引数だけを持たせ、文言の解決は `GlobalExceptionHandler` が `MessageHelper` で行う。サービス層でメッセージ文字列を組み立てない
