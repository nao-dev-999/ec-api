package com.example.ecapi.controller.admin.product.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdminProductResponse(
        Long id,
        String name,
        String description,
        BigDecimal price,
        int stock,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Integer version) {}
