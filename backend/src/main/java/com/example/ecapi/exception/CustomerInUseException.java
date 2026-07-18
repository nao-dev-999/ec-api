package com.example.ecapi.exception;

public class CustomerInUseException extends ConflictException {

    public CustomerInUseException(Object... args) {
        super(ErrorCode.CUSTOMER_IN_USE, args);
    }
}
