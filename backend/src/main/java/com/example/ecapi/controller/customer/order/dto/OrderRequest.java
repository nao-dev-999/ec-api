package com.example.ecapi.controller.customer.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record OrderRequest(
        @NotEmpty(message = "{validation.order.items.notEmpty}") @Valid
                List<OrderItemRequest> items,
        @Size(max = 30, message = "{validation.order.couponCode.size}") String couponCode,
        @NotNull(message = "{validation.order.shippingAddressId.notNull}")
                Long shippingAddressId) {}
