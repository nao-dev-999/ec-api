package com.example.ecapi.batch.job;

import com.example.ecapi.batch.dto.AggregatedSalesRow;
import com.example.ecapi.batch.dto.OrderDetailProjection;
import com.example.ecapi.batch.dto.SalesSummaryRow;
import com.example.ecapi.repository.CustomerOrderRepository;
import jakarta.persistence.EntityManagerFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import javax.sql.DataSource;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.ChunkOrientedStepBuilder;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.item.ItemStreamReader;
import org.springframework.batch.infrastructure.item.database.JdbcBatchItemWriter;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * 日次売上集計ジョブネット（14.3〜14.4節参照）。 JobA(受信I/F取込) → JobB(集計・Local Partitioning・Consolidate) →
 * JobC(送信I/F生成) の4Step構成で、 外部I/OとDB内部処理を同一Stepに混在させない。
 */
@Configuration
public class DailySalesAggregationJobConfig {

    @Bean
    public Job dailySalesAggregationJob(
            JobRepository jobRepository,
            Step jobAIntakeStep,
            Step jobBAggregateStep,
            Step jobBConsolidateStep,
            Step jobCExportStep) {
        return new JobBuilder("dailySalesAggregationJob", jobRepository)
                .start(jobAIntakeStep)
                .next(jobBAggregateStep)
                .next(jobBConsolidateStep)
                .next(jobCExportStep)
                .build();
    }

    @Bean
    public Step jobAIntakeStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            @Value("${batch.input.flag-file-template}") String flagFileTemplate) {
        Tasklet checkArrivalFlagTasklet =
                (contribution, chunkContext) -> {
                    String from =
                            String.valueOf(
                                    chunkContext
                                            .getStepContext()
                                            .getJobParameters()
                                            .get("targetDateFrom"));
                    String targetDate =
                            Instant.parse(from)
                                    .atZone(ZoneId.of("Asia/Tokyo"))
                                    .toLocalDate()
                                    .format(DateTimeFormatter.BASIC_ISO_DATE);
                    Path flag = Paths.get(String.format(flagFileTemplate, targetDate));
                    if (!Files.exists(flag)) {
                        throw new FlagFileNotFoundException("受信I/Fの到着フラグが未検出: " + flag);
                    }
                    return RepeatStatus.FINISHED;
                };
        return new StepBuilder("jobAIntakeStep", jobRepository)
                .tasklet(checkArrivalFlagTasklet, transactionManager)
                .build();
    }

    @Bean
    public Step jobBAggregateStep(
            JobRepository jobRepository,
            Step jobBWorkerStep,
            CustomerOrderRepository customerOrderRepository,
            TaskExecutor batchTaskExecutor) {
        return new StepBuilder("jobBAggregateStep", jobRepository)
                .partitioner(
                        jobBWorkerStep.getName(),
                        new OrderAggregationPartitioner(customerOrderRepository))
                .step(jobBWorkerStep)
                .taskExecutor(batchTaskExecutor)
                .gridSize(4)
                .build();
    }

    @Bean
    public Step jobBWorkerStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            ItemStreamReader<OrderDetailProjection> orderDetailReader,
            SalesSummaryItemProcessor salesSummaryProcessor,
            JdbcBatchItemWriter<SalesSummaryRow> salesSummaryStagingWriter,
            SalesSummarySkipListener salesSummarySkipListener) {
        ChunkOrientedStepBuilder<OrderDetailProjection, SalesSummaryRow> stepBuilder =
                new StepBuilder("jobBWorkerStep", jobRepository)
                        .<OrderDetailProjection, SalesSummaryRow>chunk(500)
                        .transactionManager(transactionManager)
                        .reader(orderDetailReader)
                        .processor(salesSummaryProcessor)
                        .writer(salesSummaryStagingWriter);
        return new BatchFaultTolerancePolicy().apply(stepBuilder, salesSummarySkipListener).build();
    }

    @Bean
    @StepScope
    public ItemStreamReader<OrderDetailProjection> orderDetailReader(
            EntityManagerFactory entityManagerFactory,
            @Value("#{stepExecutionContext['minId']}") Long minId,
            @Value("#{stepExecutionContext['maxId']}") Long maxId,
            @Value("#{jobParameters['targetDateFrom']}") String from,
            @Value("#{jobParameters['targetDateTo']}") String to) {
        return new OrderDetailKeysetItemReader(
                entityManagerFactory, minId, maxId, Instant.parse(from), Instant.parse(to));
    }

    @Bean
    @StepScope
    public SalesSummaryItemProcessor salesSummaryProcessor(
            @Value("#{jobParameters['targetDateFrom']}") String from) {
        return new SalesSummaryItemProcessor(
                Instant.parse(from).atZone(ZoneId.of("Asia/Tokyo")).toLocalDate());
    }

    @Bean
    @StepScope
    public SalesSummarySkipListener salesSummarySkipListener(
            DataSource dataSource,
            @Value("#{stepExecution.jobExecutionId}") Long jobExecutionId,
            @Value("#{stepExecution.stepName}") String stepName) {
        return new SalesSummarySkipListener(
                new NamedParameterJdbcTemplate(dataSource), jobExecutionId, stepName);
    }

    @Bean
    public Step jobBConsolidateStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            StagingAggregateItemReader stagingAggregateItemReader,
            JdbcBatchItemWriter<AggregatedSalesRow> dailySalesSummaryUpsertWriter,
            StagingCleanupListener stagingCleanupListener) {
        return new StepBuilder("jobBConsolidateStep", jobRepository)
                .<AggregatedSalesRow, AggregatedSalesRow>chunk(1000)
                .transactionManager(transactionManager)
                .reader(stagingAggregateItemReader)
                .writer(dailySalesSummaryUpsertWriter)
                .listener(stagingCleanupListener)
                .build();
    }

    @Bean
    @StepScope
    public StagingAggregateItemReader stagingAggregateItemReader(
            DataSource dataSource,
            @Value("#{stepExecution.jobExecution.jobInstanceId}") Long jobInstanceId) {
        return new StagingAggregateItemReader(dataSource, jobInstanceId);
    }

    @Bean
    public StagingCleanupListener stagingCleanupListener(DataSource dataSource) {
        return new StagingCleanupListener(new NamedParameterJdbcTemplate(dataSource));
    }

    @Bean
    public Step jobCExportStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            @Value("${batch.output.dir}") String outputDir) {
        Tasklet writeCompletionFlagTasklet =
                (contribution, chunkContext) -> {
                    String jobDate =
                            String.valueOf(
                                    chunkContext
                                            .getStepContext()
                                            .getJobParameters()
                                            .get("targetDateFrom"));
                    writeCompletionFlag(Paths.get(outputDir), jobDate);
                    return RepeatStatus.FINISHED;
                };
        return new StepBuilder("jobCExportStep", jobRepository)
                .tasklet(writeCompletionFlagTasklet, transactionManager)
                .build();
    }

    private void writeCompletionFlag(Path targetDir, String jobDate) throws IOException {
        Files.createDirectories(targetDir);
        Path tmp = targetDir.resolve(jobDate + ".done.tmp");
        Path fin = targetDir.resolve(jobDate + ".done");
        Files.createFile(tmp);
        Files.move(tmp, fin, StandardCopyOption.ATOMIC_MOVE); // rename は原子的操作
    }

    @Bean
    public TaskExecutor batchTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(4);
        executor.setThreadNamePrefix("batch-partition-");
        executor.initialize();
        return executor;
    }
}
