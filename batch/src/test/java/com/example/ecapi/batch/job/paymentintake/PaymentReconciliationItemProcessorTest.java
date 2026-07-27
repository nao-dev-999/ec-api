package com.example.ecapi.batch.job.paymentintake;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.ecapi.batch.dto.PaymentReconciliationRow;
import com.example.ecapi.batch.dto.PaymentUpsertRow;
import com.example.ecapi.batch.exception.PaymentReconciliationException;
import com.example.ecapi.constant.OrderPaymentStatus;
import com.example.ecapi.constant.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PaymentReconciliationItemProcessorTest {

    private final PaymentReconciliationItemProcessor processor =
            new PaymentReconciliationItemProcessor();
    private final Instant orderedAt = Instant.parse("2024-01-15T01:00:00Z");
    private final Instant settledAt = Instant.parse("2024-01-15T03:12:45Z");

    @Test
    @DisplayName("SETTLEDはCAPTURED/CAPTUREDへマッピングされ、captured_atはsettledAtになること")
    void shouldMapSettledToCaptured() {
        PaymentReconciliationRow item =
                new PaymentReconciliationRow(
                        1L,
                        orderedAt,
                        "order-1",
                        "txn-1",
                        "SETTLED",
                        new BigDecimal("1000.00"),
                        new BigDecimal("30.00"),
                        settledAt);

        PaymentUpsertRow row = processor.process(item);

        assertThat(row.status()).isEqualTo(PaymentStatus.CAPTURED);
        assertThat(row.orderPaymentStatus()).isEqualTo(OrderPaymentStatus.CAPTURED);
        assertThat(row.netAmount()).isEqualByComparingTo("970.00");
        assertThat(row.authorizedAt()).isEqualTo(orderedAt);
        assertThat(row.capturedAt()).isEqualTo(settledAt);
    }

    @Test
    @DisplayName("CANCELLEDはFAILED/CANCELLEDへマッピングされ、captured_atはnullのままになること")
    void shouldMapCancelledToFailedWithoutCapturedAt() {
        PaymentReconciliationRow item =
                new PaymentReconciliationRow(
                        1L,
                        orderedAt,
                        "order-1",
                        "txn-1",
                        "CANCELLED",
                        new BigDecimal("1000.00"),
                        new BigDecimal("30.00"),
                        settledAt);

        PaymentUpsertRow row = processor.process(item);

        assertThat(row.status()).isEqualTo(PaymentStatus.FAILED);
        assertThat(row.orderPaymentStatus()).isEqualTo(OrderPaymentStatus.CANCELLED);
        assertThat(row.capturedAt()).isNull();
    }

    @Test
    @DisplayName("REFUNDEDはREFUNDED/REFUNDEDへマッピングされ、captured_atはnullのままになること")
    void shouldMapRefundedWithoutOverwritingCapturedAt() {
        PaymentReconciliationRow item =
                new PaymentReconciliationRow(
                        1L,
                        orderedAt,
                        "order-1",
                        "txn-1",
                        "REFUNDED",
                        new BigDecimal("1000.00"),
                        new BigDecimal("30.00"),
                        settledAt);

        PaymentUpsertRow row = processor.process(item);

        assertThat(row.status()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(row.orderPaymentStatus()).isEqualTo(OrderPaymentStatus.REFUNDED);
        assertThat(row.capturedAt()).isNull();
    }

    @Test
    @DisplayName("customerOrderIdがnull（孤立レコード）の場合はPaymentReconciliationExceptionを投げること")
    void shouldThrowWhenCustomerOrderIdIsNull() {
        PaymentReconciliationRow item =
                new PaymentReconciliationRow(
                        null,
                        null,
                        "order-1",
                        "txn-1",
                        "SETTLED",
                        new BigDecimal("1000.00"),
                        new BigDecimal("30.00"),
                        settledAt);

        assertThatThrownBy(() -> processor.process(item))
                .isInstanceOf(PaymentReconciliationException.class)
                .hasMessageContaining("order-1");
    }

    @Test
    @DisplayName("statusが未知の値の場合はPaymentReconciliationExceptionを投げること")
    void shouldThrowWhenStatusIsUnknown() {
        PaymentReconciliationRow item =
                new PaymentReconciliationRow(
                        1L,
                        orderedAt,
                        "order-1",
                        "txn-1",
                        "PENDING",
                        new BigDecimal("1000.00"),
                        new BigDecimal("30.00"),
                        settledAt);

        assertThatThrownBy(() -> processor.process(item))
                .isInstanceOf(PaymentReconciliationException.class)
                .hasMessageContaining("PENDING");
    }
}
