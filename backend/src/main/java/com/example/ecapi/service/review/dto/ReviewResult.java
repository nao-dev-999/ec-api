package com.example.ecapi.service.review.dto;

import java.time.Instant;

public record ReviewResult(
        Long id,
        Long productId,
        String productName,
        Long customerId,
        String customerName,
        int rating,
        String comment,
        Instant createdAt,
        Instant updatedAt,
        int version) {}
