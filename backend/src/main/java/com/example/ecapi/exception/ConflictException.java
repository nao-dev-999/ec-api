package com.example.ecapi.exception;

public abstract class ConflictException extends BusinessException {

    protected ConflictException(ErrorCode errorCode, Object... args) {
        super(errorCode, args);
    }
}
