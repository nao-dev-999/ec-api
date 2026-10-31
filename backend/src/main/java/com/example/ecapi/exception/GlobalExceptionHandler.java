package com.example.ecapi.exception;

import com.example.ecapi.helper.MessageHelper;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final MessageHelper messageHelper;

    /**
     * ビジネス例外（BusinessException のサブクラス全て）→ 各 status
     *
     * <p>ただし JPA の OptimisticLockException は BusinessException ではないため、別ハンドラーで処理する。
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex) {
        log.warn("{}: code={}", ex.getClass().getSimpleName(), ex.getErrorCode());
        String message = messageHelper.get(ex.getErrorCode().getMessageKey(), ex.getArgs());
        return buildError(ex.getErrorCode(), message);
    }

    /**
     * 楽観ロックの競合 → 409 Conflict
     *
     * <p>楽観ロック例外はフレームワークがスローするため BusinessException 階層の外にある。
     *
     * <ul>
     *   <li>{@link OptimisticLockingFailureException}: {@code @Transactional}
     *       のコミット時やリポジトリ呼び出し時に、Spring が JPA/Hibernate の例外を変換したもの（{@code
     *       ObjectOptimisticLockingFailureException}）。実運用ではほぼこちらが届く
     *   <li>{@link jakarta.persistence.OptimisticLockException}: Spring の例外変換を通らずに届いた JPA 例外
     * </ul>
     *
     * <p>前者は {@code Exception} ハンドラーより先に一致させる必要があるため、明示的に列挙する（Spring は最上位の例外型で先に照合し、 cause はたどらない）。
     */
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
        return buildError(
                ErrorCode.VALIDATION_ERROR,
                messageHelper.get(ErrorCode.VALIDATION_ERROR.getMessageKey()),
                details);
    }

    /**
     * ログイン時の認証失敗（メールアドレス・パスワード不一致等）→ 401 Unauthorized
     *
     * <p>コントローラー内で {@code AuthenticationManager}
     * を呼んだときにスローされるものだけがここに来る。未認証アクセス・権限不足（フィルターで弾かれるもの）は {@code SecurityConfig} の {@code
     * exceptionHandling()} で処理する。
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException ex) {
        log.warn("AuthenticationException: {}", ex.getMessage());
        return buildError(
                ErrorCode.AUTHENTICATION_FAILED,
                messageHelper.get(ErrorCode.AUTHENTICATION_FAILED.getMessageKey()));
    }

    /** リクエストボディの型不正・不正なenum値など、JSONパース失敗 → 400 Bad Request */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMessageNotReadable(
            HttpMessageNotReadableException ex) {
        log.warn("HttpMessageNotReadableException: {}", ex.getMessage());
        return buildError(
                ErrorCode.BAD_REQUEST, messageHelper.get(ErrorCode.BAD_REQUEST.getMessageKey()));
    }

    /** パスパラメータの型不正 → 400 Bad Request */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex) {
        log.warn("MethodArgumentTypeMismatchException: {}", ex.getMessage());
        return buildError(
                ErrorCode.BAD_REQUEST, messageHelper.get(ErrorCode.BAD_REQUEST.getMessageKey()));
    }

    /** 予期しない例外 → 500 Internal Server Error（詳細は隠してログに残す） */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
        log.error("Unexpected exception occurred", ex);
        return buildError(
                ErrorCode.SYSTEM_ERROR, messageHelper.get(ErrorCode.SYSTEM_ERROR.getMessageKey()));
    }

    private ResponseEntity<ErrorResponse> buildError(ErrorCode code, String message) {
        return buildError(code, message, null);
    }

    private ResponseEntity<ErrorResponse> buildError(
            ErrorCode code, String message, Map<String, String> details) {
        return ResponseEntity.status(code.getStatus())
                .body(ErrorResponse.of(code, message, details));
    }
}
