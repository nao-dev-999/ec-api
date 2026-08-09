package com.example.ecapi.controller.customer.product.dto;

import java.util.List;

public record ProductReviewsResponse(List<ReviewResponse> reviews, ReviewSummaryResponse summary) {}
