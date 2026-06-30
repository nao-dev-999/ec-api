package com.example.ecapi.controller.customer.cart.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AddCartItemRequest(
        @NotNull(message = "{validation.cart.productId.notNull}") Long productId,
        @Min(value = 1, message = "{validation.cart.quantity.min}") int quantity) {}
