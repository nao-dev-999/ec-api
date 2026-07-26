package com.example.ecapi.batch.dto;

import java.math.BigDecimal;
import java.time.Instant;

/** 決済システム向け出力ファイル（settlement_detail_YYYYMMDD.csv）の1行分。CSVの列順と一致させる。 */
public record PaymentSettlementRow(
        Long orderId,
        String paymentId,
        Instant capturedAt,
        BigDecimal amount,
        BigDecimal fee,
        BigDecimal netAmount,
        String settlementCycle) {}
