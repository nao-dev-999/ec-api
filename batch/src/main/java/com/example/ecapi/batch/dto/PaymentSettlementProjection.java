package com.example.ecapi.batch.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 決済システム（入金消込用）向け出力ファイルのReaderが返す1行分の射影。 {@code id}はPaymentの内部PKで、{@link
 * com.example.ecapi.batch.reader.PaymentSettlementKeysetItemReader}の
 * キーセットページングのカーソルにのみ使用し、CSV出力そのものには含めない。
 */
public record PaymentSettlementProjection(
        Long id,
        Long orderId,
        String paymentId,
        Instant capturedAt,
        BigDecimal amount,
        BigDecimal fee,
        BigDecimal netAmount) {}
