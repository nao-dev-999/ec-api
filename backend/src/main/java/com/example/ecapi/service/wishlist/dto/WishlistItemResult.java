package com.example.ecapi.service.wishlist.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record WishlistItemResult(
        Long id,
        Long productId,
        String productName,
        BigDecimal price,
        int stock,
        LocalDateTime createdAt) {}
