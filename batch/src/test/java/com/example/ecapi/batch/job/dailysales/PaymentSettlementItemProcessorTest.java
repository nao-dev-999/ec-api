package com.example.ecapi.batch.job.dailysales;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.ecapi.batch.dto.PaymentSettlementProjection;
import com.example.ecapi.batch.dto.PaymentSettlementRow;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PaymentSettlementItemProcessorTest {

    private final PaymentSettlementItemProcessor processor =
            new PaymentSettlementItemProcessor("20240115");

    @Test
    @DisplayName("PAYMENTの射影をsettlement_cycle付きのPaymentSettlementRowへ変換すること")
    void shouldMapProjectionToRowWithSettlementCycle() {
        Instant capturedAt = Instant.parse("2024-01-15T03:12:45Z");
        PaymentSettlementProjection item =
                new PaymentSettlementProjection(
                        1L,
                        10L,
                        "txn-1",
                        capturedAt,
                        new BigDecimal("1000.00"),
                        new BigDecimal("30.00"),
                        new BigDecimal("970.00"));

        PaymentSettlementRow row = processor.process(item);

        assertThat(row.orderId()).isEqualTo(10L);
        assertThat(row.paymentId()).isEqualTo("txn-1");
        assertThat(row.capturedAt()).isEqualTo(capturedAt);
        assertThat(row.amount()).isEqualByComparingTo("1000.00");
        assertThat(row.fee()).isEqualByComparingTo("30.00");
        assertThat(row.netAmount()).isEqualByComparingTo("970.00");
        assertThat(row.settlementCycle()).isEqualTo("20240115");
    }
}
