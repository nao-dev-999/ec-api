package com.example.ecapi.exception;

/** 無効化済み・有効期限外・利用上限到達・当該顧客が使用済みのクーポンを適用しようとした場合の例外 */
public class CouponNotAllowedException extends ConflictException {

    public CouponNotAllowedException(Object... args) {
        super(ErrorCode.COUPON_NOT_ALLOWED, args);
    }
}
