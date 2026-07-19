package com.example.ecapi.batch.dto;

import java.math.BigDecimal;

public record OrderDetailProjection(
        Long id, Long productId, Long customerId, BigDecimal unitPrice, int quantity) {}
