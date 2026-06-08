package com.example.ecapi.controller.customer.order.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        String customerName,
        String status,
        BigDecimal totalAmount,
        List<OrderItemResponse> items,
        LocalDateTime orderedAt,
        LocalDateTime updatedAt,
        Integer version) {}
