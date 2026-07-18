package com.example.ecapi.exception;

public class OrderCannotBeCancelledException extends ConflictException {

    public OrderCannotBeCancelledException(Object... args) {
        super(ErrorCode.ORDER_CANNOT_BE_CANCELLED, args);
    }
}
