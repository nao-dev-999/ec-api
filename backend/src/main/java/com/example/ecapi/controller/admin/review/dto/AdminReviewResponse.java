package com.example.ecapi.controller.admin.review.dto;

import java.time.Instant;

public record AdminReviewResponse(
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
