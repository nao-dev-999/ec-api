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
}
