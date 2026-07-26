package com.example.ecapi.batch.dto;

import com.example.ecapi.constant.OrderPaymentStatus;
import com.example.ecapi.constant.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;

/** paymentテーブルへのUPSERTと、customer_order.payment_status更新の両方に必要な値を保持する。 */
public record PaymentUpsertRow(
        Long customerOrderId,
        String transactionId,
        PaymentStatus status,
        OrderPaymentStatus orderPaymentStatus,
        BigDecimal amount,
        BigDecimal fee,
        BigDecimal netAmount,
        Instant authorizedAt,
        Instant capturedAt) {}
