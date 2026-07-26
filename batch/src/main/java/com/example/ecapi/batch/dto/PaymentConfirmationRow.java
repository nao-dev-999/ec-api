package com.example.ecapi.batch.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentConfirmationRow(
        String orderNumber,
        String transactionId,
        Long customerId,
        String paymentMethod,
        String status,
        BigDecimal amount,
        BigDecimal fee,
        Instant settledAt) {}
