package com.example.ecapi.exception;

public class ProductIdMismatchException extends BusinessException {

    public ProductIdMismatchException() {
        super(ErrorCode.PRODUCT_ID_MISMATCH);
    }
}
