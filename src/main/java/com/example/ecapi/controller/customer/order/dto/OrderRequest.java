package com.example.ecapi.controller.customer.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record OrderRequest(
        @NotBlank(message = "{validation.order.customerId.notBlank}") String customerId,
        @NotBlank(message = "{validation.order.customerName.notBlank}") String customerName,
        @NotEmpty(message = "{validation.order.items.notEmpty}") @Valid
                List<OrderItemRequest> items) {}
