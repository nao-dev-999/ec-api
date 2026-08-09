package com.example.ecapi.service.review.dto;

public record UpdateReview(
        Long reviewId, Long customerId, int rating, String comment, int version) {}
