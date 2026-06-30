package com.example.ecapi.exception;

public class CategoryNotFoundException extends ResourceNotFoundException {

    public CategoryNotFoundException(Object... args) {
        super(ErrorCode.CATEGORY_NOT_FOUND, args);
    }
}
