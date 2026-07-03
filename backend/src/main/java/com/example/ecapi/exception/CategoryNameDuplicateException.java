package com.example.ecapi.exception;

public class CategoryNameDuplicateException extends ConflictException {

    public CategoryNameDuplicateException(Object... args) {
        super(ErrorCode.CATEGORY_NAME_DUPLICATE, args);
    }
}
