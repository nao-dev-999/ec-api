package com.example.ecapi.controller.order.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;

@Builder
public record OrderResponse(
        Long id,
        String customerName,
        String status,
        BigDecimal totalAmount,
        List<OrderItemResponse> items,
        LocalDateTime orderedAt,
        LocalDateTime updatedAt,
        Integer version) {}
