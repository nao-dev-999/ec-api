package com.example.ecapi.controller.customer.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record OrderRequest(
        @NotEmpty(message = "{validation.order.items.notEmpty}") @Valid
                List<OrderItemRequest> items) {}
