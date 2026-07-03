package com.example.ecapi.exception;

public class CartItemNotFoundException extends ResourceNotFoundException {

    public CartItemNotFoundException(Object... args) {
        super(ErrorCode.CART_ITEM_NOT_FOUND, args);
    }
}
