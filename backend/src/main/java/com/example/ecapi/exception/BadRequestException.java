package com.example.ecapi.exception;

import org.springframework.http.HttpStatus;

/** 400 Bad Request を返すビジネス例外の中間抽象クラス。ステータスはここで固定する。 */
public abstract class BadRequestException extends BusinessException {

    protected BadRequestException(ErrorCode errorCode, Object... args) {
        super(requireStatus(errorCode, HttpStatus.BAD_REQUEST), args);
    }
}
