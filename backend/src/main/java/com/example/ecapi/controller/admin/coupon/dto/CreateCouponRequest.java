package com.example.ecapi.controller.admin.coupon.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CreateCouponRequest(
        @NotBlank(message = "{validation.coupon.code.notBlank}")
                @Size(max = 30, message = "{validation.coupon.code.size}")
                String code,
        @NotNull(message = "{validation.coupon.discountAmount.notNull}")
                @DecimalMin(
                        value = "0.0",
                        inclusive = false,
                        message = "{validation.coupon.discountAmount.min}")
                BigDecimal discountAmount,
        LocalDateTime validFrom,
        LocalDateTime validTo,
        @Min(value = 1, message = "{validation.coupon.usageLimit.min}") Integer usageLimit) {}
