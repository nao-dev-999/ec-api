package com.example.ecapi.exception;

public class CouponNotFoundException extends ResourceNotFoundException {

    public CouponNotFoundException(Object... args) {
        super(ErrorCode.COUPON_NOT_FOUND, args);
    }
}
