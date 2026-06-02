package com.example.ecapi.exception;

import com.example.ecapi.helper.MessageHelper;
import jakarta.persistence.OptimisticLockException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * グローバル例外ハンドラー
 *
 * <p>API で発生した例外を適切な HTTP ステータスと JSON レスポンスに統一して変換する。 全ハンドラーが {@link ErrorResponse} を返す。
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final MessageHelper messageHelper;

    /** バリデーションエラー → 400 Bad Request */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, WebRequest request) {
        Map<String, String> details =
                ex.getBindingResult().getFieldErrors().stream()
                        .collect(
                                Collectors.toMap(
                                        FieldError::getField,
                                        FieldError::getDefaultMessage,
                                        (existing, duplicate) ->
                                                existing)); // 同一フィールドに複数エラーがある場合は先勝ち
        log.warn("Validation error: {}", details);
        return buildError(HttpStatus.BAD_REQUEST, messageHelper.get("error.validation"), details);
    }

    /** 商品が見つからないエラー → 404 Not Found */
    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleProductNotFoundException(
            ProductNotFoundException ex, WebRequest request) {
        log.warn("ProductNotFoundException: {}", ex.getMessage());
        return buildError(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /** 注文が見つからないエラー → 404 Not Found */
    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleOrderNotFoundException(
            OrderNotFoundException ex, WebRequest request) {
        log.warn("OrderNotFoundException: {}", ex.getMessage());
        return buildError(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /** 在庫不足エラー → 409 Conflict */
    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientStockException(
            InsufficientStockException ex, WebRequest request) {
        log.warn("InsufficientStockException: {}", ex.getMessage());
        return buildError(HttpStatus.CONFLICT, ex.getMessage());
    }

    /** パスパラメータの型不正 → 400 Bad Request */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, WebRequest request) {
        log.warn("MethodArgumentTypeMismatchException: {}", ex.getMessage());
        return buildError(HttpStatus.BAD_REQUEST, messageHelper.get("error.badRequest"));
    }

    @ExceptionHandler(OptimisticLockException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(
            OptimisticLockException ex, WebRequest request) {
        log.warn("OptimisticLockException: {}", ex.getMessage());
        return buildError(HttpStatus.CONFLICT, ex.getMessage());
    }

    /** その他サーバーエラー → 500 Internal Server Error */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex, WebRequest request) {
        log.error("Unexpected exception occurred", ex);
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, messageHelper.get("error.system"));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthException(AuthenticationException ex) {
        return buildError(HttpStatus.UNAUTHORIZED, "error.authentication");
    }

    private ResponseEntity<ErrorResponse> buildError(HttpStatus status, String message) {
        return buildError(status, message, null);
    }

    private ResponseEntity<ErrorResponse> buildError(
            HttpStatus status, String message, Map<String, String> details) {
        ErrorResponse body =
                new ErrorResponse(
                        LocalDateTime.now(),
                        status.value(),
                        status.getReasonPhrase(),
                        message,
                        details);
        return ResponseEntity.status(status).body(body);
    }
}
