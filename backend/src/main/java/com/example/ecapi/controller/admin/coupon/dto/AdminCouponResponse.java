package com.example.ecapi.controller.admin.coupon.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdminCouponResponse(
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
