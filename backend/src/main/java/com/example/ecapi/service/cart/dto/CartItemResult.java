package com.example.ecapi.service.cart.dto;

import java.math.BigDecimal;

public record CartItemResult(
        Long id,
        Long customerId,
        Long productId,
        String productName,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal subtotal,
        int version) {}
