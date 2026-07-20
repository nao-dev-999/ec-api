package com.example.ecapi.batch.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
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
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@ExtendWith(MockitoExtension.class)
class StagingCleanupListenerTest {

    @Mock private NamedParameterJdbcTemplate jdbcTemplate;
    @Mock private StepExecution stepExecution;
    @Mock private JobExecution jobExecution;

    @Test
    @DisplayName("Stepが成功した場合はjob_instance_id分のステージング行をDELETEすること")
    void shouldDeleteStagingRowsWhenStepCompleted() {
        when(stepExecution.getStatus()).thenReturn(BatchStatus.COMPLETED);
        when(stepExecution.getJobExecution()).thenReturn(jobExecution);
        when(jobExecution.getJobInstanceId()).thenReturn(42L);

        StagingCleanupListener listener = new StagingCleanupListener(jdbcTemplate);
        listener.afterStep(stepExecution);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(jdbcTemplate).update(anyString(), paramsCaptor.capture());
        assertThat(paramsCaptor.getValue()).containsEntry("jobInstanceId", 42L);
    }

    @Test
    @DisplayName("Stepが失敗した場合はDELETEしないこと")
    void shouldNotDeleteStagingRowsWhenStepFailed() {
        when(stepExecution.getStatus()).thenReturn(BatchStatus.FAILED);

        StagingCleanupListener listener = new StagingCleanupListener(jdbcTemplate);
        listener.afterStep(stepExecution);

        verify(jdbcTemplate, never()).update(anyString(), anyMap());
    }
}
