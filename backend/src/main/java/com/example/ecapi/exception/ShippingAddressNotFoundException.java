package com.example.ecapi.exception;

public class ShippingAddressNotFoundException extends ResourceNotFoundException {

    public ShippingAddressNotFoundException(Object... args) {
        super(ErrorCode.SHIPPING_ADDRESS_NOT_FOUND, args);
    }
}
