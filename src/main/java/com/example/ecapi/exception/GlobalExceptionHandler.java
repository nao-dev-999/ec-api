package com.example.ecapi.exception;

import com.example.ecapi.helper.MessageHelper;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
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
     * JPA の楽観ロック例外 → 409 Conflict
     *
     * <p>jakarta.persistence.OptimisticLockException は JPA フレームワークがスローするため BusinessException
     * 階層の外にある。ここでメッセージを付与して 409 として返す。
     */
    @ExceptionHandler(jakarta.persistence.OptimisticLockException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLock(
            jakarta.persistence.OptimisticLockException ex) {
        log.warn("OptimisticLockException: {}", ex.getMessage());
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
        var body =
                new ErrorResponse(
                        Instant.now(),
                        code.getStatus().value(),
                        code.getStatus().getReasonPhrase(),
                        code.name(),
                        message,
                        details);
        return ResponseEntity.status(code.getStatus()).body(body);
    }
}
