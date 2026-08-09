package com.example.ecapi.controller.customer.wishlist.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record WishlistItemResponse(
        Long id,
        Long productId,
        String productName,
        BigDecimal price,
        int stock,
        LocalDateTime createdAt) {}
