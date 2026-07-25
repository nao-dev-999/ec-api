package com.example.ecapi.batch.exception;

public class FlagFileNotFoundException extends RuntimeException {

    public FlagFileNotFoundException(String message) {
        super(message);
    }
}
