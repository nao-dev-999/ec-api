package com.example.ecapi.exception;

public class InvalidCurrentPasswordException extends BadRequestException {

    public InvalidCurrentPasswordException() {
        super(ErrorCode.INVALID_CURRENT_PASSWORD);
    }
}
