package com.example.ecapi.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
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
    @Mock private Job paymentIntakeJob;
    @Mock private Job monthlyReportJob;
    @Mock private JobExecution jobExecution;
    @Mock private JobParametersProvider paymentIntakeJobParametersProvider;
    @Mock private JobParametersProvider monthlyReportJobParametersProvider;

    @Test
    @DisplayName("--job未指定時は例外を投げること")
    void shouldThrowWhenJobArgOmitted() throws Exception {
        BatchRunner runner =
                new BatchRunner(
                        jobOperator,
                        jobRepository,
                        Map.of(
                                "paymentIntakeJob", paymentIntakeJob,
                                "monthlyReportJob", monthlyReportJob),
                        List.of());

        assertThatThrownBy(runner::run)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("--job=");

        verify(jobOperator, never()).start(any(Job.class), any(JobParameters.class));
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
                                "paymentIntakeJob", paymentIntakeJob,
                                "monthlyReportJob", monthlyReportJob),
                        List.of(
                                paymentIntakeJobParametersProvider,
                                monthlyReportJobParametersProvider));

        runner.run("--job=monthlyReportJob");

        verify(jobOperator).start(eq(monthlyReportJob), any(JobParameters.class));
        assertThat(runner.getExitCode()).isEqualTo(0);
    }

    @Test
    @DisplayName("JobExecutionが失敗した場合はexitCodeが1になること")
    void shouldSetExitCodeOneWhenJobExecutionFailed() throws Exception {
        when(paymentIntakeJob.getName()).thenReturn("paymentIntakeJob");
        when(jobRepository.findRunningJobExecutions("paymentIntakeJob")).thenReturn(Set.of());
        when(paymentIntakeJobParametersProvider.jobName()).thenReturn("paymentIntakeJob");
        when(paymentIntakeJobParametersProvider.resolve(any()))
                .thenReturn(new JobParametersBuilder().toJobParameters());
        when(jobOperator.start(eq(paymentIntakeJob), any(JobParameters.class)))
                .thenReturn(jobExecution);
        when(jobExecution.getStatus()).thenReturn(BatchStatus.FAILED);

        BatchRunner runner =
                new BatchRunner(
                        jobOperator,
                        jobRepository,
                        Map.of("paymentIntakeJob", paymentIntakeJob),
                        List.of(paymentIntakeJobParametersProvider));

        runner.run("--job=paymentIntakeJob");

        assertThat(runner.getExitCode()).isEqualTo(1);
    }

    @Test
    @DisplayName("未知のJob名を指定した場合は例外を投げること")
    void shouldThrowWhenJobNameUnknown() {
        BatchRunner runner =
                new BatchRunner(
                        jobOperator,
                        jobRepository,
                        Map.of("paymentIntakeJob", paymentIntakeJob),
                        List.of());

        assertThatThrownBy(() -> runner.run("--job=unknownJob"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unknownJob");
    }

    @Test
    @DisplayName("同名Jobが既に実行中の場合は起動を中止し例外を投げること")
    void shouldThrowWhenJobAlreadyRunning() throws Exception {
        when(paymentIntakeJob.getName()).thenReturn("paymentIntakeJob");
        when(jobRepository.findRunningJobExecutions("paymentIntakeJob"))
                .thenReturn(Set.of(jobExecution));

        BatchRunner runner =
                new BatchRunner(
                        jobOperator,
                        jobRepository,
                        Map.of("paymentIntakeJob", paymentIntakeJob),
                        List.of(paymentIntakeJobParametersProvider));

        assertThatThrownBy(() -> runner.run("--job=paymentIntakeJob"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("paymentIntakeJob");

        verify(jobOperator, never()).start(any(Job.class), any(JobParameters.class));
    }

    @Test
    @DisplayName("JobParametersProviderが未登録のJobを起動しようとすると例外を投げること")
    void shouldThrowWhenParametersProviderMissing() {
        when(paymentIntakeJob.getName()).thenReturn("paymentIntakeJob");
        when(jobRepository.findRunningJobExecutions("paymentIntakeJob")).thenReturn(Set.of());

        BatchRunner runner =
                new BatchRunner(
                        jobOperator,
                        jobRepository,
                        Map.of("paymentIntakeJob", paymentIntakeJob),
                        List.of());

        assertThatThrownBy(() -> runner.run("--job=paymentIntakeJob"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("paymentIntakeJob");
    }
}
