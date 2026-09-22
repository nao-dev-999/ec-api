package com.example.ecapi.controller.customer.product.dto;

import com.example.ecapi.controller.common.dto.PageResponse;

public record ProductReviewsResponse(
        PageResponse<ReviewResponse> reviews, ReviewSummaryResponse summary) {}
