package com.example.ecapi.controller.customer.cart.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateCartItemQuantityRequest(
        @Min(value = 1, message = "{validation.cart.quantity.min}") int quantity,
        @NotNull int version) {}
