package com.example.ecapi.controller.customer.wishlist.dto;

import jakarta.validation.constraints.NotNull;

public record AddWishlistItemRequest(
        @NotNull(message = "{validation.wishlist.productId.notNull}") Long productId) {}
