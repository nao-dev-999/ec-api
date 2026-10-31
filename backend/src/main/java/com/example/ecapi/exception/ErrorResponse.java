package com.example.ecapi.exception;

import java.time.Instant;
import java.util.Map;

public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String code,
        String message,
        Map<String, String> details) {

    /** details なし（通常エラー用） */
    public ErrorResponse(Instant timestamp, int status, String error, String code, String message) {
        this(timestamp, status, error, code, message, null);
    }

    /**
     * {@link ErrorCode} からエラーレスポンスを組み立てる。
     *
     * <p>{@code GlobalExceptionHandler} と {@code FilterErrorResponseWriter} の両方がこれを使い、レスポンス形式をそろえる。
     */
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
