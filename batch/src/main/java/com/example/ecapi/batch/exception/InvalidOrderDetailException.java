package com.example.ecapi.batch.exception;

public class InvalidOrderDetailException extends RuntimeException {

    public InvalidOrderDetailException(String message) {
        super(message);
    }
}
