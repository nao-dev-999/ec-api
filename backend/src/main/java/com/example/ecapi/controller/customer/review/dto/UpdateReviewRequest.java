package com.example.ecapi.controller.customer.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateReviewRequest(
        @NotNull(message = "{validation.review.rating.notNull}")
                @Min(value = 1, message = "{validation.review.rating.min}")
                @Max(value = 5, message = "{validation.review.rating.max}")
                Integer rating,
        @Size(max = 1000, message = "{validation.review.comment.size}") String comment,
        int version) {}
