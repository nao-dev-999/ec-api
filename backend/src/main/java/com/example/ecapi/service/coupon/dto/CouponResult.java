package com.example.ecapi.service.coupon.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CouponResult(
        Long id,
        String code,
        BigDecimal discountAmount,
        LocalDateTime validFrom,
        LocalDateTime validTo,
        Integer usageLimit,
        int usageCount,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Integer version) {}
