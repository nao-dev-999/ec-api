package com.example.ecapi.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;

@ExtendWith(MockitoExtension.class)
class BatchRunnerTest {

    @Mock private JobOperator jobOperator;
    @Mock private JobRepository jobRepository;
    @Mock private Job dailySalesAggregationJob;
    @Mock private Job monthlyReportJob;
    @Mock private JobExecution jobExecution;
    @Mock private JobParametersProvider dailySalesJobParametersProvider;
    @Mock private JobParametersProvider monthlyReportJobParametersProvider;

    @Test
    @DisplayName("--job未指定時はdailySalesAggregationJobがデフォルトで起動されること")
    void shouldRunDefaultJobWhenJobArgOmitted() throws Exception {
        when(dailySalesAggregationJob.getName()).thenReturn("dailySalesAggregationJob");
        when(jobRepository.findRunningJobExecutions("dailySalesAggregationJob"))
                .thenReturn(Set.of());
        when(dailySalesJobParametersProvider.jobName()).thenReturn("dailySalesAggregationJob");
        when(dailySalesJobParametersProvider.resolve(any()))
                .thenReturn(new JobParametersBuilder().toJobParameters());
        when(jobOperator.start(eq(dailySalesAggregationJob), any(JobParameters.class)))
                .thenReturn(jobExecution);
        when(jobExecution.getStatus()).thenReturn(BatchStatus.COMPLETED);

        BatchRunner runner =
                new BatchRunner(
                        jobOperator,
                        jobRepository,
                        Map.of(
                                "dailySalesAggregationJob", dailySalesAggregationJob,
                                "monthlyReportJob", monthlyReportJob),
                        List.of(
                                dailySalesJobParametersProvider,
                                monthlyReportJobParametersProvider));

        runner.run();

        verify(jobOperator).start(eq(dailySalesAggregationJob), any(JobParameters.class));
        assertThat(runner.getExitCode()).isEqualTo(0);
    }

    @Test
    @DisplayName("--jobで指定したJobが起動されること")
    void shouldRunJobSpecifiedByArg() throws Exception {
        when(monthlyReportJob.getName()).thenReturn("monthlyReportJob");
        when(jobRepository.findRunningJobExecutions("monthlyReportJob")).thenReturn(Set.of());
        when(monthlyReportJobParametersProvider.jobName()).thenReturn("monthlyReportJob");
        when(monthlyReportJobParametersProvider.resolve(any()))
                .thenReturn(new JobParametersBuilder().toJobParameters());
        when(jobOperator.start(eq(monthlyReportJob), any(JobParameters.class)))
                .thenReturn(jobExecution);
        when(jobExecution.getStatus()).thenReturn(BatchStatus.COMPLETED);

        BatchRunner runner =
                new BatchRunner(
                        jobOperator,
                        jobRepository,
                        Map.of(
                                "dailySalesAggregationJob", dailySalesAggregationJob,
                                "monthlyReportJob", monthlyReportJob),
                        List.of(
                                dailySalesJobParametersProvider,
                                monthlyReportJobParametersProvider));

        runner.run("--job=monthlyReportJob");

        verify(jobOperator).start(eq(monthlyReportJob), any(JobParameters.class));
        assertThat(runner.getExitCode()).isEqualTo(0);
    }

    @Test
    @DisplayName("JobExecutionが失敗した場合はexitCodeが1になること")
    void shouldSetExitCodeOneWhenJobExecutionFailed() throws Exception {
        when(dailySalesAggregationJob.getName()).thenReturn("dailySalesAggregationJob");
        when(jobRepository.findRunningJobExecutions("dailySalesAggregationJob"))
                .thenReturn(Set.of());
        when(dailySalesJobParametersProvider.jobName()).thenReturn("dailySalesAggregationJob");
        when(dailySalesJobParametersProvider.resolve(any()))
                .thenReturn(new JobParametersBuilder().toJobParameters());
        when(jobOperator.start(eq(dailySalesAggregationJob), any(JobParameters.class)))
                .thenReturn(jobExecution);
        when(jobExecution.getStatus()).thenReturn(BatchStatus.FAILED);

        BatchRunner runner =
                new BatchRunner(
                        jobOperator,
                        jobRepository,
                        Map.of("dailySalesAggregationJob", dailySalesAggregationJob),
                        List.of(dailySalesJobParametersProvider));

        runner.run();

        assertThat(runner.getExitCode()).isEqualTo(1);
    }

    @Test
    @DisplayName("未知のJob名を指定した場合は例外を投げること")
    void shouldThrowWhenJobNameUnknown() {
        BatchRunner runner =
                new BatchRunner(
                        jobOperator,
                        jobRepository,
                        Map.of("dailySalesAggregationJob", dailySalesAggregationJob),
                        List.of());

        assertThatThrownBy(() -> runner.run("--job=unknownJob"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unknownJob");
    }

    @Test
    @DisplayName("JobParametersProviderが未登録のJobを起動しようとすると例外を投げること")
    void shouldThrowWhenParametersProviderMissing() {
        when(dailySalesAggregationJob.getName()).thenReturn("dailySalesAggregationJob");
        when(jobRepository.findRunningJobExecutions("dailySalesAggregationJob"))
                .thenReturn(Set.of());

        BatchRunner runner =
                new BatchRunner(
                        jobOperator,
                        jobRepository,
                        Map.of("dailySalesAggregationJob", dailySalesAggregationJob),
                        List.of());

        assertThatThrownBy(runner::run)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("dailySalesAggregationJob");
    }
}
