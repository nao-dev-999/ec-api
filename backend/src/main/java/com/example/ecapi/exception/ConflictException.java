package com.example.ecapi.exception;

import org.springframework.http.HttpStatus;

/** 409 Conflict を返すビジネス例外の中間抽象クラス。ステータスはここで固定する。 */
public abstract class ConflictException extends BusinessException {

    protected ConflictException(ErrorCode errorCode, Object... args) {
        super(requireStatus(errorCode, HttpStatus.CONFLICT), args);
    }
}
