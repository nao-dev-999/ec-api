package com.example.ecapi.exception;

public class ProductIdMismatchException extends BadRequestException {

    public ProductIdMismatchException() {
        super(ErrorCode.PRODUCT_ID_MISMATCH);
    }
}
