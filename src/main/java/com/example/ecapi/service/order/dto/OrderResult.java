package com.example.ecapi.service.order.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResult(
        Long id,
        String customerName,
        String status,
        BigDecimal totalAmount,
        List<OrderResultItem> items,
        LocalDateTime orderedAt,
        LocalDateTime updatedAt,
        Integer version) {}
