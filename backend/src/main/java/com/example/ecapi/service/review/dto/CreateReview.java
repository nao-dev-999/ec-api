package com.example.ecapi.service.review.dto;

public record CreateReview(Long customerId, Long productId, int rating, String comment) {}
