package com.example.ecapi.controller.admin.order.dto;

import java.math.BigDecimal;

public record AdminOrderItemResponse(
        Long productId,
        String productName,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal) {}
