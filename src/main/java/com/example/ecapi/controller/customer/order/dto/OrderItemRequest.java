package com.example.ecapi.controller.customer.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record OrderItemRequest(
        @NotNull Long productId,
        @Min(value = 1, message = "{validation.orderItem.quantity.min}") int quantity) {}
