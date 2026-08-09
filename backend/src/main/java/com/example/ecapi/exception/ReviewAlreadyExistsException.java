package com.example.ecapi.exception;

public class ReviewAlreadyExistsException extends ConflictException {

    public ReviewAlreadyExistsException(Object... args) {
        super(ErrorCode.REVIEW_ALREADY_EXISTS, args);
    }
}
