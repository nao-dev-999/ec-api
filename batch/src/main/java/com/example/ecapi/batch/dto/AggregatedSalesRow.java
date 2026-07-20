package com.example.ecapi.batch.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AggregatedSalesRow(
        Long productId, LocalDate salesDate, BigDecimal totalAmount, int totalQuantity) {}
