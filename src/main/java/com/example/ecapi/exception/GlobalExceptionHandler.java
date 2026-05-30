package com.example.ecapi.exception;

import com.example.ecapi.config.MessageHelper;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest; // WebRequest をインポート
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * グローバル例外ハンドラー
 *
 * <p>API で発生した例外を適切な HTTP ステータスと JSON レスポンスに統一して変換する。
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final MessageHelper messageHelper;

    /** バリデーションエラー → 400 Bad Request */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult()
                .getAllErrors()
                .forEach(
                        error -> {
                            String field = ((FieldError) error).getField();
                            errors.put(field, error.getDefaultMessage());
                        });
        return buildError(HttpStatus.BAD_REQUEST, messageHelper.get("error.validation"), errors);
    }

    /**
     * 注文が見つからないエラー → 404 Not Found OrderNotFoundException は @ResponseStatus で 404 が設定されているが、
     * より詳細なエラーレスポンスを返すために明示的にハンドリングする。
     */
    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleOrderNotFoundException(
            OrderNotFoundException ex, WebRequest request) {
        ErrorResponse errorResponse =
                new ErrorResponse(
                        LocalDateTime.now(),
                        HttpStatus.NOT_FOUND.value(),
                        HttpStatus.NOT_FOUND.getReasonPhrase(),
                        ex.getMessage(),
                        request.getDescription(false).replace("uri=", ""));
        log.warn("OrderNotFoundException: {}", ex.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    /** 商品が見つからないエラー → 404 Not Found */
    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleProductNotFoundException(
            ProductNotFoundException ex, WebRequest request) {
        ErrorResponse errorResponse =
                new ErrorResponse(
                        LocalDateTime.now(),
                        HttpStatus.NOT_FOUND.value(),
                        HttpStatus.NOT_FOUND.getReasonPhrase(),
                        ex.getMessage(),
                        request.getDescription(false).replace("uri=", ""));
        log.warn("ProductNotFoundException: {}", ex.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    /** 在庫不足エラー → 409 Conflict */
    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<Map<String, Object>> handleInsufficientStockException(
            InsufficientStockException ex, WebRequest request) {
        log.warn("InsufficientStockException: {}", ex.getMessage());
        return buildError(HttpStatus.CONFLICT, ex.getMessage());
    }

    /** リクエストデータマッピングエラー → 404 Not Found */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(
            MethodArgumentTypeMismatchException ex, WebRequest request) {
        log.warn("MethodArgumentTypeMismatchException: {}", ex.getMessage());
        return buildError(HttpStatus.NOT_FOUND);
    }

    /** 在庫不足などビジネスルール違反 → 409 Conflict */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleBusiness(IllegalStateException ex) {
        log.warn("IllegalStateException: {}", ex.getMessage());
        return buildError(HttpStatus.CONFLICT);
    }

    /** その他サーバーエラー → 500 Internal Server Error */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception ex) {
        log.error("Unexpected exception occurred", ex);
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, messageHelper.get("error.system"));
    }

    private ResponseEntity<Map<String, Object>> buildError(HttpStatus status) {
        return buildError(status, null);
    }

    private ResponseEntity<Map<String, Object>> buildError(HttpStatus status, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        if (message != null) {
            body.put("detail", message);
        }
        return ResponseEntity.status(status).body(body);
    }

    private ResponseEntity<Map<String, Object>> buildError(
            HttpStatus status, String message, Object detail) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", message);
        if (detail != null) {
            body.put("detail", detail);
        }
        return ResponseEntity.status(status).body(body);
    }
}
