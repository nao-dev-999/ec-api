package com.example.ecapi.controller.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public record OrderRequest(
        @NotBlank(message = "{validation.order.customerName.notBlank}") String customerName,
        @NotEmpty(message = "{validation.order.items.notEmpty}") @Valid List<OrderItemRequest> items) {}
