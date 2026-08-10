package com.example.ecapi.exception;

public class WishlistItemNotFoundException extends ResourceNotFoundException {

    public WishlistItemNotFoundException(Object... args) {
        super(ErrorCode.WISHLIST_ITEM_NOT_FOUND, args);
    }
}
