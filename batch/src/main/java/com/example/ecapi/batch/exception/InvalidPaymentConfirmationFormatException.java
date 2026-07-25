package com.example.ecapi.batch.exception;

public class InvalidPaymentConfirmationFormatException extends RuntimeException {

    public InvalidPaymentConfirmationFormatException(String message) {
        super(message);
    }
}
