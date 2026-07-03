package com.example.ecapi.exception;

public abstract class ResourceNotFoundException extends BusinessException {

    protected ResourceNotFoundException(ErrorCode errorCode, Object... args) {
        super(errorCode, args);
    }
}
