package com.example.ecapi.batch.job.salesaggregation;

import com.example.ecapi.batch.dto.AggregatedSalesRow;
import com.example.ecapi.batch.dto.OrderDetailProjection;
import com.example.ecapi.batch.dto.SalesSummaryRow;
import jakarta.persistence.EntityManagerFactory;
import java.time.Instant;
import java.time.ZoneId;
import javax.sql.DataSource;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.partition.Partitioner;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemStreamReader;
import org.springframework.batch.infrastructure.item.database.JdbcBatchItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * 売上集計ジョブ（旧dailySalesAggregationJobの集計フェーズ）。 CustomerOrderDetailをLocal
 * Partitioningで並列集計しステージングへINSERT後、 job_instance_id単位でConsolidateして daily_sales_summary_*
 * テーブルへUPSERTする。 決済突合（paymentIntakeJob）の完了有無はこのJobでは関知しない。
 *
 * <p>salesAggregateWorkerStepはpaymentIntakeJobの後続であり、決済データの不正はpaymentIntakeJob側で
 * 既に排除されている前提のため、データ不正（{@link
 * com.example.ecapi.batch.exception.InvalidOrderDetailException}）はskip対象にしない。金銭データを扱う集計処理では、
 * 想定外のデータを1件でも無視せず即座にStep/Jobを異常終了させて調査に回す方針とする（一時的なシステムエラー（{@link
 * TransientDataAccessException}）のみretryで区別する）。
 */
@Configuration
public class SalesAggregationJobConfig {

    @Bean
    public Job salesAggregationJob(
            JobRepository jobRepository,
            Step salesAggregatePartitionStep,
            Step salesSummaryConsolidateStep) {
        return new JobBuilder("salesAggregationJob", jobRepository)
                .start(salesAggregatePartitionStep)
                .next(salesSummaryConsolidateStep)
                .build();
    }

    @Bean
    public Step salesAggregatePartitionStep(
            JobRepository jobRepository,
            Step salesAggregateWorkerStep,
            Partitioner orderAggregationPartitioner,
            TaskExecutor batchTaskExecutor) {
        return new StepBuilder("salesAggregatePartitionStep", jobRepository)
                .partitioner(salesAggregateWorkerStep.getName(), orderAggregationPartitioner)
                .step(salesAggregateWorkerStep)
                .taskExecutor(batchTaskExecutor)
                .gridSize(4)
                .build();
    }

    @Bean
    @StepScope
    public Partitioner orderAggregationPartitioner(
            DataSource dataSource,
            @Value("#{jobParameters['targetDateFrom']}") String from,
            @Value("#{jobParameters['targetDateTo']}") String to) {
        return new OrderAggregationPartitioner(
                new NamedParameterJdbcTemplate(dataSource), Instant.parse(from), Instant.parse(to));
    }

    @Bean
    public Step salesAggregateWorkerStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            ItemStreamReader<OrderDetailProjection> orderDetailReader,
            SalesSummaryItemProcessor salesSummaryProcessor,
            JdbcBatchItemWriter<SalesSummaryRow> salesSummaryStagingWriter) {
        return new StepBuilder("salesAggregateWorkerStep", jobRepository)
                .<OrderDetailProjection, SalesSummaryRow>chunk(500)
                .transactionManager(transactionManager)
                .reader(orderDetailReader)
                .processor(salesSummaryProcessor)
                .writer(salesSummaryStagingWriter)
                .faultTolerant()
                .retry(TransientDataAccessException.class)
                .retryLimit(3)
                .build();
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
    public Step salesSummaryConsolidateStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            StagingAggregateItemReader stagingAggregateItemReader,
            JdbcBatchItemWriter<AggregatedSalesRow> dailySalesSummaryUpsertWriter,
            StagingCleanupListener stagingCleanupListener) {
        return new StepBuilder("salesSummaryConsolidateStep", jobRepository)
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
    public TaskExecutor batchTaskExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("batch-partition-");
        executor.setVirtualThreads(true);
        return executor;
    }
}
