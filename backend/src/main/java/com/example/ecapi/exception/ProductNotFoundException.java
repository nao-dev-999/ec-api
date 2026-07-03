package com.example.ecapi.exception;

public class ProductNotFoundException extends ResourceNotFoundException {

    public ProductNotFoundException(Object... args) {
        super(ErrorCode.PRODUCT_NOT_FOUND, args);
    }
}
