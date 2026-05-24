package com.example.ecapi.service.product.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductResult(
        Long id,
        String name,
        String description,
        BigDecimal price,
        int stock,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Integer version) {}
