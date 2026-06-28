# 6. 例外処理

> [← インデックスに戻る](../コーディング規約.md)

---

## 6.1 例外クラス階層

全てのビジネス例外は `BusinessException` を頂点とする階層で管理する。
`BusinessException` 自身が `HttpStatus` を保持するため、`GlobalExceptionHandler` は型ごとにハンドラーを増やさず1本で処理できる。

```
BusinessException (abstract)          ← 全ビジネス例外の親。HttpStatus を保持
  ├── ResourceNotFoundException (abstract)  ← 404 固定
  │     ├── ProductNotFoundException
  │     └── OrderNotFoundException
  └── ConflictException (abstract)         ← 409 固定
        ├── InsufficientStockException
        └── OptimisticLockConflictException
```

### BusinessException（基底クラス）

```java
// exception/BusinessException.java
public abstract class BusinessException extends RuntimeException {

    private final HttpStatus status;

    protected BusinessException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
```

### 中間抽象クラス

HTTP ステータスをここで固定する。個別の具象クラスはメッセージを受け取るだけでよい。

```java
// exception/ResourceNotFoundException.java
public abstract class ResourceNotFoundException extends BusinessException {
    protected ResourceNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}

// exception/ConflictException.java
public abstract class ConflictException extends BusinessException {
    protected ConflictException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
```

### 具象例外クラス

```java
// exception/ProductNotFoundException.java
public class ProductNotFoundException extends ResourceNotFoundException {
    public ProductNotFoundException(String message) {
        super(message);
    }
}

// exception/OrderNotFoundException.java
public class OrderNotFoundException extends ResourceNotFoundException {
    public OrderNotFoundException(String message) {
        super(message);
    }
}

// exception/InsufficientStockException.java
public class InsufficientStockException extends ConflictException {
    public InsufficientStockException(String message) {
        super(message);
    }
}

// exception/OptimisticLockConflictException.java
// jakarta.persistence.OptimisticLockException（JPA例外）とは別物。
// Hibernate がスローする JPA例外を GlobalExceptionHandler でラップしてこちらに変換する。
public class OptimisticLockConflictException extends ConflictException {
    public OptimisticLockConflictException(String message) {
        super(message);
    }
}
```

**新しいビジネス例外を追加するときのルール:**

1. `ResourceNotFoundException` か `ConflictException` の適切な中間クラスを継承する
2. どちらにも当てはまらない HTTP ステータスが必要な場合は、同じパターンで新たな中間抽象クラスを追加する（例: `BadRequestException` → 400）
3. 具象クラスは `message` を受け取るコンストラクタ1つのみ持つ

---

## 6.2 ErrorResponse

```java
// exception/ErrorResponse.java
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        Map<String, String> details) {

    /** details なし（通常エラー用） */
    public ErrorResponse(Instant timestamp, int status, String error, String message) {
        this(timestamp, status, error, message, null);
    }
}
```

- `timestamp` は `Instant`（UTC）を使用する。`LocalDateTime` は使わない
- `details` はバリデーションエラー時のみ使用し、それ以外は `null`

---

## 6.3 GlobalExceptionHandler

```java
// exception/GlobalExceptionHandler.java
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final MessageHelper messageHelper;

    /**
     * ビジネス例外（BusinessException のサブクラス全て）→ 各 status
     *
     * <p>ただし JPA の OptimisticLockException は BusinessException ではないため、
     * 別ハンドラーで処理する。
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex) {
        log.warn("{}: {}", ex.getClass().getSimpleName(), ex.getMessage());
        return buildError(ex.getStatus(), ex.getMessage());
    }

    /**
     * JPA の楽観ロック例外 → 409 Conflict
     *
     * <p>jakarta.persistence.OptimisticLockException は JPA フレームワークがスローするため
     * BusinessException 階層の外にある。ここでメッセージを付与して 409 として返す。
     */
    @ExceptionHandler(jakarta.persistence.OptimisticLockException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLock(
            jakarta.persistence.OptimisticLockException ex) {
        log.warn("OptimisticLockException: {}", ex.getMessage());
        return buildError(HttpStatus.CONFLICT, messageHelper.get("error.conflict.optimisticLock"));
    }

    /** バリデーションエラー → 400 Bad Request */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> details =
                ex.getBindingResult().getFieldErrors().stream()
                        .collect(
                                Collectors.toMap(
                                        FieldError::getField,
                                        FieldError::getDefaultMessage,
                                        (existing, duplicate) -> existing));
        log.warn("Validation error: {}", details);
        return buildError(HttpStatus.BAD_REQUEST, messageHelper.get("error.validation"), details);
    }

    /** パスパラメータの型不正 → 400 Bad Request */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex) {
        log.warn("MethodArgumentTypeMismatchException: {}", ex.getMessage());
        return buildError(HttpStatus.BAD_REQUEST, messageHelper.get("error.badRequest"));
    }

    /** 予期しない例外 → 500 Internal Server Error（詳細は隠してログに残す） */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
        log.error("Unexpected exception occurred", ex);
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, messageHelper.get("error.system"));
    }

    private ResponseEntity<ErrorResponse> buildError(HttpStatus status, String message) {
        return buildError(status, message, null);
    }

    private ResponseEntity<ErrorResponse> buildError(
            HttpStatus status, String message, Map<String, String> details) {
        var body =
                new ErrorResponse(
                        Instant.now(), status.value(), status.getReasonPhrase(), message, details);
        return ResponseEntity.status(status).body(body);
    }
}
```

---

## 6.4 原則まとめ

- **ビジネス例外は必ず `BusinessException` を継承する。** `RuntimeException` を直接継承しない
- **HTTPステータスは中間抽象クラスで固定する。** 具象クラスにステータスを書かない。`GlobalExceptionHandler` にも書かない
- **`GlobalExceptionHandler` に `BusinessException` のサブクラス個別のハンドラーを追加しない。** 新しい例外クラスを追加しても `handleBusiness` が自動的に処理する
- **Spring Security の 401/403 は `@ControllerAdvice` で処理できない。** `SecurityConfig` の `exceptionHandling()` で `AuthenticationEntryPoint` / `AccessDeniedHandler` を設定する
- **500 は詳細を隠してログに残す。** `handleGeneral` では `ex.getMessage()` をレスポンスに含めない
- **例外メッセージは `messages.properties` 経由で取得する。** サービス層では `messageHelper.get(key, args...)` を使う
