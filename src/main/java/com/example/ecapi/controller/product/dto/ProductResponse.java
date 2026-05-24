package com.example.ecapi.controller.product.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductResponse(
        Long id,
        String name,
        String description,
        BigDecimal price,
        int stock,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Integer version) {}
