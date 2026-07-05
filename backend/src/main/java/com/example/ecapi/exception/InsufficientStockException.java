package com.example.ecapi.exception;

public class InsufficientStockException extends ConflictException {

    public InsufficientStockException(Object... args) {
        super(ErrorCode.INSUFFICIENT_STOCK, args);
    }
}
