package com.example.ecapi.controller.customer.product.dto;

import java.time.Instant;

public record ReviewResponse(
        Long id,
        Long productId,
        Long customerId,
        String customerName,
        int rating,
        String comment,
        Instant createdAt,
        Instant updatedAt,
        int version) {}
