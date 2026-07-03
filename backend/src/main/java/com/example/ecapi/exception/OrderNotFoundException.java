package com.example.ecapi.exception;

public class OrderNotFoundException extends ResourceNotFoundException {

    public OrderNotFoundException(Object... args) {
        super(ErrorCode.ORDER_NOT_FOUND, args);
    }
}
