package com.example.ecapi.service.order.dto;

import java.math.BigDecimal;

public record OrderResultItem(
        Long productId,
        String productName,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal) {}
