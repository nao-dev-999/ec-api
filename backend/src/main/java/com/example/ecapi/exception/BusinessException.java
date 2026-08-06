package com.example.ecapi.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public abstract class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;
    private final transient Object[] args;

    protected BusinessException(ErrorCode errorCode, Object... args) {
        super(errorCode.getMessageKey());
        this.errorCode = errorCode;
        this.args = args == null ? new Object[0] : args;
    }

    public HttpStatus getStatus() {
        return errorCode.getStatus();
    }
}
