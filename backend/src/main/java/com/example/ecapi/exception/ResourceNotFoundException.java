package com.example.ecapi.exception;

import org.springframework.http.HttpStatus;

/** 404 Not Found を返すビジネス例外の中間抽象クラス。ステータスはここで固定する。 */
public abstract class ResourceNotFoundException extends BusinessException {

    protected ResourceNotFoundException(ErrorCode errorCode, Object... args) {
        super(requireStatus(errorCode, HttpStatus.NOT_FOUND), args);
    }
}
