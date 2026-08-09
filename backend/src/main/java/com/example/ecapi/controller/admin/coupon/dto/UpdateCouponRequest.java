package com.example.ecapi.controller.admin.coupon.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * PUT /api/admin/coupons/{id} 用の更新リクエスト（部分更新） discountAmount/usageLimit/active は null ==
 * 変更しない（versionは楽観ロックのため必須）
 */
public record UpdateCouponRequest(
        @DecimalMin(
                        value = "0.0",
                        inclusive = false,
                        message = "{validation.coupon.discountAmount.min}")
                BigDecimal discountAmount,
        LocalDateTime validFrom,
        LocalDateTime validTo,
        @Min(value = 1, message = "{validation.coupon.usageLimit.min}") Integer usageLimit,
        Boolean active,
        int version) {}
