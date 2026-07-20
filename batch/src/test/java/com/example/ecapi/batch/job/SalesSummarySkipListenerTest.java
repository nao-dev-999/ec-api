package com.example.ecapi.batch.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.example.ecapi.batch.dto.OrderDetailProjection;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@ExtendWith(MockitoExtension.class)
class SalesSummarySkipListenerTest {

    @Mock private NamedParameterJdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("onSkipInProcessでbatch_skipped_recordsへ期待通りの内容をINSERTすること")
    void shouldRecordSkippedItemOnSkipInProcess() {
        SalesSummarySkipListener listener =
                new SalesSummarySkipListener(jdbcTemplate, 99L, "jobBWorkerStep");
        OrderDetailProjection item =
                new OrderDetailProjection(7L, 100L, 200L, new BigDecimal("-1"), 3);

        listener.onSkipInProcess(item, new InvalidOrderDetailException("unitPriceが不正です"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(jdbcTemplate)
                .update(org.mockito.ArgumentMatchers.anyString(), paramsCaptor.capture());

        assertThat(paramsCaptor.getValue())
                .containsEntry("jobExecutionId", 99L)
                .containsEntry("stepName", "jobBWorkerStep")
                .containsEntry("orderDetailId", 7L)
                .containsEntry("errorMessage", "unitPriceが不正です");
    }
}
