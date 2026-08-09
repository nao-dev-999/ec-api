package com.example.ecapi.service.coupon.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Service 層で扱う Update 用 DTO（部分更新） null 値は「変更しない」を意味します（ただし validFrom/validTo は null で無期限化を表すため対象外）。
 */
public record UpdateCoupon(
        Long id,
        BigDecimal discountAmount,
        LocalDateTime validFrom,
        LocalDateTime validTo,
        Integer usageLimit,
        Boolean active,
        int version) {}
