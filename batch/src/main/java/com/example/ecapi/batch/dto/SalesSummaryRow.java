package com.example.ecapi.batch.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SalesSummaryRow(
        Long productId, LocalDate salesDate, BigDecimal amount, int quantity) {}
