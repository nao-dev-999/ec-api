package com.example.ecapi.batch;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.stereotype.Component;

/**
 * spring.batch.job.enabled=false により標準のJobLauncherApplicationRunnerを無効化した上で、
 * このRunnerが唯一の起動経路になる（14.6節: 二重起動防止、14.8節: JobParametersによる冪等性）。
 */
@Component
public class DailySalesBatchRunner implements CommandLineRunner, ExitCodeGenerator {

    private static final ZoneId JST = ZoneId.of("Asia/Tokyo");
    private static final String TARGET_DATE_ARG_PREFIX = "--targetDate=";

    private final JobOperator jobOperator;
    private final JobRepository jobRepository;
    private final Job dailySalesAggregationJob;

    private volatile int exitCode = 0;

    public DailySalesBatchRunner(
            JobOperator jobOperator, JobRepository jobRepository, Job dailySalesAggregationJob) {
        this.jobOperator = jobOperator;
        this.jobRepository = jobRepository;
        this.dailySalesAggregationJob = dailySalesAggregationJob;
    }

    @Override
    public void run(String... args) throws Exception {
        String jobName = dailySalesAggregationJob.getName();
        if (!jobRepository.findRunningJobExecutions(jobName).isEmpty()) {
            throw new IllegalStateException("ジョブが既に実行中のため起動を中止します: " + jobName);
        }

        LocalDate targetDate = resolveTargetDate(args);
        Instant from = targetDate.atStartOfDay(JST).toInstant();
        Instant to = targetDate.plusDays(1).atStartOfDay(JST).toInstant();

        JobParameters jobParameters =
                new JobParametersBuilder()
                        .addString("targetDateFrom", from.toString())
                        .addString("targetDateTo", to.toString())
                        .toJobParameters();

        JobExecution execution = jobOperator.start(dailySalesAggregationJob, jobParameters);
        exitCode = execution.getStatus() == BatchStatus.COMPLETED ? 0 : 1;
    }

    private LocalDate resolveTargetDate(String[] args) {
        for (String arg : args) {
            if (arg.startsWith(TARGET_DATE_ARG_PREFIX)) {
                return LocalDate.parse(arg.substring(TARGET_DATE_ARG_PREFIX.length()));
            }
        }
        return LocalDate.now(JST).minusDays(1); // 未指定時は「前日分」（14.2節）
    }

    @Override
    public int getExitCode() {
        return exitCode;
    }
}
