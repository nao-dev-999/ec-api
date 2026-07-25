package com.example.ecapi.batch.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentConfirmationRow(Long orderId, BigDecimal amount, Instant settledAt) {}
