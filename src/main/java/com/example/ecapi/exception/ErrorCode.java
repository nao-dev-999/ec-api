package com.example.ecapi.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    // 404 Not Found
    PRODUCT_NOT_FOUND("product.notFound", HttpStatus.NOT_FOUND),
    ORDER_NOT_FOUND("order.notFound", HttpStatus.NOT_FOUND),

    // 409 Conflict
    INSUFFICIENT_STOCK("order.insufficientStock", HttpStatus.CONFLICT),
    OPTIMISTIC_LOCK_CONFLICT("error.conflict.optimisticLock", HttpStatus.CONFLICT),

    // 400 Bad Request
    VALIDATION_ERROR("error.validation", HttpStatus.BAD_REQUEST),
    BAD_REQUEST("error.badRequest", HttpStatus.BAD_REQUEST),

    // 500 Internal Server Error
    SYSTEM_ERROR("error.system", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String messageKey;
    private final HttpStatus status;

    ErrorCode(String messageKey, HttpStatus status) {
        this.messageKey = messageKey;
        this.status = status;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
