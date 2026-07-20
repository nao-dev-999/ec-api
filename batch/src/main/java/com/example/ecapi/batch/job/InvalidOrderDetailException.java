package com.example.ecapi.batch.job;

public class InvalidOrderDetailException extends RuntimeException {

    public InvalidOrderDetailException(String message) {
        super(message);
    }
}
