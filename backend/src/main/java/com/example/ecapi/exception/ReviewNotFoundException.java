package com.example.ecapi.exception;

public class ReviewNotFoundException extends ResourceNotFoundException {

    public ReviewNotFoundException(Object... args) {
        super(ErrorCode.REVIEW_NOT_FOUND, args);
    }
}
