package com.example.ecapi.service.coupon.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CreateCoupon(
        String code,
        BigDecimal discountAmount,
        LocalDateTime validFrom,
        LocalDateTime validTo,
        Integer usageLimit) {}
