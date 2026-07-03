package com.example.ecapi.exception;

public class CustomerNotFoundException extends ResourceNotFoundException {

    public CustomerNotFoundException(Object... args) {
        super(ErrorCode.CUSTOMER_NOT_FOUND, args);
    }
}
