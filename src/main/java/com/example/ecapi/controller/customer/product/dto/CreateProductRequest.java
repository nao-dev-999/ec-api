package com.example.ecapi.controller.customer.product.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CreateProductRequest(
        @NotBlank(message = "{validation.product.name.notBlank}") String name,
        String description,
        @NotNull(message = "{validation.product.price.notNull}")
                @DecimalMin(
                        value = "0.0",
                        inclusive = false,
                        message = "{validation.product.price.decimalMin}")
                BigDecimal price,
        @Min(value = 0, message = "{validation.product.stock.min}") int stock) {}
