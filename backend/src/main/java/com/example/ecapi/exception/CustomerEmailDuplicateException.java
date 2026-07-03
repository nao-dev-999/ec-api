package com.example.ecapi.exception;

public class CustomerEmailDuplicateException extends ConflictException {

    public CustomerEmailDuplicateException(Object... args) {
        super(ErrorCode.CUSTOMER_EMAIL_DUPLICATE, args);
    }
}
