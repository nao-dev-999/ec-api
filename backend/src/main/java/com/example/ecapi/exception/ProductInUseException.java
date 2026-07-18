package com.example.ecapi.exception;

public class ProductInUseException extends ConflictException {

    public ProductInUseException(Object... args) {
        super(ErrorCode.PRODUCT_IN_USE, args);
    }
}
