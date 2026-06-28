package com.example.ecapi.service.order.dto;

import com.example.ecapi.constant.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResult(
        Long id,
        String customerName,
        OrderStatus status,
        BigDecimal totalAmount,
        List<OrderResultItem> items,
        LocalDateTime orderedAt,
        LocalDateTime updatedAt,
        Integer version) {}
