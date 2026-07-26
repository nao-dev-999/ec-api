package com.example.ecapi.batch.job.dailysales;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@ExtendWith(MockitoExtension.class)
class PaymentReconciliationAlertJobListenerTest {

    @Mock private NamedParameterJdbcTemplate jdbcTemplate;
    @Mock private JobExecution jobExecution;

    @Test
    @DisplayName("Jobが正常完了かつアラートが1件以上ある場合、ExitStatusをPARTIAL_SUCCESS_WITH_ALERTSにすること")
    void shouldMarkExitStatusWhenAlertsExist() {
        when(jobExecution.getStatus()).thenReturn(BatchStatus.COMPLETED);
        when(jobExecution.getId()).thenReturn(123L);
        when(jdbcTemplate.queryForObject(anyString(), anyMap(), eq(Long.class))).thenReturn(2L);

        PaymentReconciliationAlertJobListener listener =
                new PaymentReconciliationAlertJobListener(jdbcTemplate);
        listener.afterJob(jobExecution);

        ArgumentCaptor<ExitStatus> exitStatusCaptor = ArgumentCaptor.forClass(ExitStatus.class);
        verify(jobExecution).setExitStatus(exitStatusCaptor.capture());
        assertThat(exitStatusCaptor.getValue().getExitCode())
                .isEqualTo(
                        PaymentReconciliationAlertJobListener
                                .EXIT_STATUS_PARTIAL_SUCCESS_WITH_ALERTS);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(jdbcTemplate).queryForObject(anyString(), paramsCaptor.capture(), eq(Long.class));
        assertThat(paramsCaptor.getValue()).containsEntry("jobExecutionId", 123L);
    }

    @Test
    @DisplayName("アラートが0件の場合はExitStatusを変更しないこと")
    void shouldNotChangeExitStatusWhenNoAlerts() {
        when(jobExecution.getStatus()).thenReturn(BatchStatus.COMPLETED);
        when(jobExecution.getId()).thenReturn(123L);
        when(jdbcTemplate.queryForObject(anyString(), anyMap(), eq(Long.class))).thenReturn(0L);

        PaymentReconciliationAlertJobListener listener =
                new PaymentReconciliationAlertJobListener(jdbcTemplate);
        listener.afterJob(jobExecution);

        verify(jobExecution, never()).setExitStatus(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("Jobが正常完了していない場合はアラート件数を確認しないこと")
    void shouldNotCheckAlertsWhenJobNotCompleted() {
        when(jobExecution.getStatus()).thenReturn(BatchStatus.FAILED);

        PaymentReconciliationAlertJobListener listener =
                new PaymentReconciliationAlertJobListener(jdbcTemplate);
        listener.afterJob(jobExecution);

        verify(jdbcTemplate, never()).queryForObject(anyString(), anyMap(), eq(Long.class));
        verify(jobExecution, never()).setExitStatus(org.mockito.ArgumentMatchers.any());
    }
}
