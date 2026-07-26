package com.example.ecapi.batch.job.dailysales;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.example.ecapi.batch.dto.PaymentReconciliationRow;
import com.example.ecapi.batch.exception.PaymentReconciliationException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@ExtendWith(MockitoExtension.class)
class PaymentReconciliationSkipListenerTest {

    @Mock private NamedParameterJdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("onSkipInProcessでpayment_reconciliation_alertsへ期待通りの内容をINSERTすること")
    void shouldRecordSkippedItemOnSkipInProcess() {
        PaymentReconciliationSkipListener listener =
                new PaymentReconciliationSkipListener(jdbcTemplate, 99L);
        PaymentReconciliationRow item =
                new PaymentReconciliationRow(
                        null,
                        null,
                        "order-1",
                        "txn-1",
                        "SETTLED",
                        new BigDecimal("1000.00"),
                        new BigDecimal("30.00"),
                        Instant.parse("2024-01-15T03:12:45Z"));

        listener.onSkipInProcess(item, new PaymentReconciliationException("オーダーが存在しません"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(jdbcTemplate)
                .update(org.mockito.ArgumentMatchers.anyString(), paramsCaptor.capture());

        assertThat(paramsCaptor.getValue())
                .containsEntry("jobExecutionId", 99L)
                .containsEntry("orderNumber", "order-1")
                .containsEntry("transactionId", "txn-1")
                .containsEntry("errorMessage", "オーダーが存在しません");
    }
}
