package com.example.ecapi.exception;

/** 配送完了済みの購入実績がない商品へのレビュー投稿を試みた場合の例外 */
public class ReviewNotAllowedException extends ConflictException {

    public ReviewNotAllowedException(Object... args) {
        super(ErrorCode.REVIEW_NOT_ALLOWED, args);
    }
}
