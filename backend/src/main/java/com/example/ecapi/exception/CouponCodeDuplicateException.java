package com.example.ecapi.exception;

public class CouponCodeDuplicateException extends ConflictException {

    public CouponCodeDuplicateException(Object... args) {
        super(ErrorCode.COUPON_CODE_DUPLICATE, args);
    }
}
