package com.example.ecapi.controller.customer.order.dto;

import com.example.ecapi.constant.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        String customerName,
        OrderStatus status,
        BigDecimal totalAmount,
        List<OrderItemResponse> items,
        LocalDateTime orderedAt,
        LocalDateTime updatedAt,
        Integer version) {}
