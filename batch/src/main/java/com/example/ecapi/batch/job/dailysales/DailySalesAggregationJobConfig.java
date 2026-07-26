package com.example.ecapi.batch.job.dailysales;

import com.example.ecapi.batch.dto.AggregatedSalesRow;
import com.example.ecapi.batch.dto.OrderDetailProjection;
import com.example.ecapi.batch.dto.PaymentConfirmationRow;
import com.example.ecapi.batch.dto.PaymentReconciliationRow;
import com.example.ecapi.batch.dto.PaymentSettlementProjection;
import com.example.ecapi.batch.dto.PaymentSettlementRow;
import com.example.ecapi.batch.dto.PaymentUpsertRow;
import com.example.ecapi.batch.dto.SalesSummaryRow;
import com.example.ecapi.batch.exception.FlagFileNotFoundException;
import com.example.ecapi.batch.exception.InvalidPaymentConfirmationFormatException;
import com.example.ecapi.batch.exception.PaymentReconciliationException;
import com.example.ecapi.batch.reader.OrderDetailKeysetItemReader;
import com.example.ecapi.batch.reader.PaymentReconciliationItemReader;
import com.example.ecapi.batch.reader.PaymentSettlementKeysetItemReader;
import com.example.ecapi.batch.reader.StagingAggregateItemReader;
import jakarta.persistence.EntityManagerFactory;
import java.io.IOException;
import java.io.UncheckedIOException;
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
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.infrastructure.item.file.FlatFileItemWriter;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.batch.infrastructure.item.support.CompositeItemWriter;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * 日次売上集計ジョブネット（14.3〜14.4節参照）。 取込フェーズ(受信I/F取込: フラグ確認→フォーマット検証・ステージング取込) → 集計フェーズ(集計・Local
 * Partitioning・Consolidate) → 送信フェーズ(送信I/F生成) の5Step構成で、 外部I/OとDB内部処理を同一Stepに混在させない。
 */
@Configuration
public class DailySalesAggregationJobConfig {

    private static final String EXPECTED_CSV_HEADER =
            "order_number,transaction_id,customer_id,payment_method,status,amount,fee,settled_at";

    @Bean
    public Job dailySalesAggregationJob(
            JobRepository jobRepository,
            Step arrivalFlagCheckStep,
            Step paymentConfirmationIntakeStep,
            Step paymentReconciliationStep,
            Step salesAggregatePartitionStep,
            Step salesSummaryConsolidateStep,
            Step settlementDetailExportStep,
            Step settlementDetailFlagExportStep,
            Step completionFlagExportStep,
            PaymentReconciliationAlertJobListener paymentReconciliationAlertJobListener) {
        return new JobBuilder("dailySalesAggregationJob", jobRepository)
                .start(arrivalFlagCheckStep)
                .next(paymentConfirmationIntakeStep)
                .next(paymentReconciliationStep)
                .next(salesAggregatePartitionStep)
                .next(salesSummaryConsolidateStep)
                .next(settlementDetailExportStep)
                .next(settlementDetailFlagExportStep)
                .next(completionFlagExportStep)
                .listener(paymentReconciliationAlertJobListener)
                .build();
    }

    @Bean
    public Step arrivalFlagCheckStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            @Value("${batch.input.flag-file-template}") String flagFileTemplate) {
        Tasklet checkArrivalFlagTasklet =
                (contribution, chunkContext) -> {
                    String targetDate =
                            targetDateYyyyMMdd(
                                    chunkContext
                                            .getStepContext()
                                            .getJobParameters()
                                            .get("targetDateFrom"));
                    Path flag = Paths.get(String.format(flagFileTemplate, targetDate));
                    if (!Files.exists(flag)) {
                        throw new FlagFileNotFoundException("受信I/Fの到着フラグが未検出: " + flag);
                    }
                    return RepeatStatus.FINISHED;
                };
        return new StepBuilder("arrivalFlagCheckStep", jobRepository)
                .tasklet(checkArrivalFlagTasklet, transactionManager)
                .build();
    }

    @Bean
    public Step paymentConfirmationIntakeStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            FlatFileItemReader<PaymentConfirmationRow> paymentConfirmationFileReader,
            JdbcBatchItemWriter<PaymentConfirmationRow> paymentConfirmationStagingWriter) {
        return new StepBuilder("paymentConfirmationIntakeStep", jobRepository)
                .<PaymentConfirmationRow, PaymentConfirmationRow>chunk(500)
                .transactionManager(transactionManager)
                .reader(paymentConfirmationFileReader)
                .writer(paymentConfirmationStagingWriter)
                .build();
    }

    @Bean
    @StepScope
    public FlatFileItemReader<PaymentConfirmationRow> paymentConfirmationFileReader(
            @Value("${batch.input.data-file-template}") String dataFileTemplate,
            @Value("#{jobParameters['targetDateFrom']}") String from) {
        String targetDate = targetDateYyyyMMdd(from);
        Path csv = Paths.get(String.format(dataFileTemplate, targetDate));
        return new FlatFileItemReaderBuilder<PaymentConfirmationRow>()
                .name("paymentConfirmationFileReader")
                .resource(new FileSystemResource(csv))
                .strict(true)
                .linesToSkip(1)
                .skippedLinesCallback(
                        header -> {
                            if (!EXPECTED_CSV_HEADER.equals(header.strip())) {
                                throw new InvalidPaymentConfirmationFormatException(
                                        "受信I/Fのヘッダー形式が不正です。期待値: "
                                                + EXPECTED_CSV_HEADER
                                                + " 実際: "
                                                + header);
                            }
                        })
                .delimited()
                .names(
                        "order_number",
                        "transaction_id",
                        "customer_id",
                        "payment_method",
                        "status",
                        "amount",
                        "fee",
                        "settled_at")
                .fieldSetMapper(new PaymentConfirmationFieldSetMapper())
                .build();
    }

    private static String targetDateYyyyMMdd(Object jobParametersTargetDateFrom) {
        return Instant.parse(String.valueOf(jobParametersTargetDateFrom))
                .atZone(ZoneId.of("Asia/Tokyo"))
                .toLocalDate()
                .format(DateTimeFormatter.BASIC_ISO_DATE);
    }

    /**
     * payment_confirmation_stagingをcustomer_orderへ突合し、paymentへUPSERTするStep。
     * 「決済ファイルにはあるがオーダーが存在しない」「statusが未知の値」はskip対象とし、 Job全体は止めずアラート記録のうえ人手調査へ回す（{@link
     * PaymentReconciliationSkipListener}参照）。
     */
    @Bean
    public Step paymentReconciliationStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            ItemStreamReader<PaymentReconciliationRow> paymentReconciliationItemReader,
            PaymentReconciliationItemProcessor paymentReconciliationItemProcessor,
            CompositeItemWriter<PaymentUpsertRow> paymentReconciliationWriter,
            PaymentReconciliationSkipListener paymentReconciliationSkipListener) {
        ChunkOrientedStepBuilder<PaymentReconciliationRow, PaymentUpsertRow> stepBuilder =
                new StepBuilder("paymentReconciliationStep", jobRepository)
                        .<PaymentReconciliationRow, PaymentUpsertRow>chunk(500)
                        .transactionManager(transactionManager)
                        .reader(paymentReconciliationItemReader)
                        .processor(paymentReconciliationItemProcessor)
                        .writer(paymentReconciliationWriter);
        return stepBuilder
                .faultTolerant()
                .skip(PaymentReconciliationException.class)
                .skipLimit(100)
                .retry(TransientDataAccessException.class)
                .retryLimit(3)
                .skipListener(paymentReconciliationSkipListener)
                .build();
    }

    @Bean
    @StepScope
    public ItemStreamReader<PaymentReconciliationRow> paymentReconciliationItemReader(
            DataSource dataSource,
            @Value("#{stepExecution.jobExecution.jobInstanceId}") Long jobInstanceId) {
        return new PaymentReconciliationItemReader(dataSource, jobInstanceId);
    }

    @Bean
    public PaymentReconciliationItemProcessor paymentReconciliationItemProcessor() {
        return new PaymentReconciliationItemProcessor();
    }

    @Bean
    @StepScope
    public PaymentReconciliationSkipListener paymentReconciliationSkipListener(
            DataSource dataSource, @Value("#{stepExecution.jobExecutionId}") Long jobExecutionId) {
        return new PaymentReconciliationSkipListener(
                new NamedParameterJdbcTemplate(dataSource), jobExecutionId);
    }

    @Bean
    public PaymentReconciliationAlertJobListener paymentReconciliationAlertJobListener(
            DataSource dataSource) {
        return new PaymentReconciliationAlertJobListener(
                new NamedParameterJdbcTemplate(dataSource));
    }

    @Bean
    public Step salesAggregatePartitionStep(
            JobRepository jobRepository,
            Step salesAggregateWorkerStep,
            DataSource dataSource,
            TaskExecutor batchTaskExecutor) {
        return new StepBuilder("salesAggregatePartitionStep", jobRepository)
                .partitioner(
                        salesAggregateWorkerStep.getName(),
                        new OrderAggregationPartitioner(new NamedParameterJdbcTemplate(dataSource)))
                .step(salesAggregateWorkerStep)
                .taskExecutor(batchTaskExecutor)
                .gridSize(4)
                .build();
    }

    @Bean
    public Step salesAggregateWorkerStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            ItemStreamReader<OrderDetailProjection> orderDetailReader,
            SalesSummaryItemProcessor salesSummaryProcessor,
            JdbcBatchItemWriter<SalesSummaryRow> salesSummaryStagingWriter,
            SalesSummarySkipListener salesSummarySkipListener) {
        ChunkOrientedStepBuilder<OrderDetailProjection, SalesSummaryRow> stepBuilder =
                new StepBuilder("salesAggregateWorkerStep", jobRepository)
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

    /**
     * 決済システム（自社の別システム、入金消込用）向けの決済明細ファイルを生成するStep。
     * daily_sales_summary_by_product（商品単位の集計値。payment_id・fee・net_amountを持たない）は経由せず、
     * PAYMENTテーブル（status = CAPTURED）から直接抽出する。1オーダー1決済のためPAYMENTの対象日分の件数規模は
     * CustomerOrderと同程度（ピーク日想定で最大20万件、14.2節参照）となりうるため、salesAggregateWorkerStepと
     * 同様にchunk指向StepとStatelessSession + キーセットページングのReaderを使う（Taskletで1トランザクションに まとめない）。
     */
    @Bean
    public Step settlementDetailExportStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            ItemStreamReader<PaymentSettlementProjection> paymentSettlementReader,
            PaymentSettlementItemProcessor paymentSettlementItemProcessor,
            FlatFileItemWriter<PaymentSettlementRow> settlementDetailFileWriter) {
        return new StepBuilder("settlementDetailExportStep", jobRepository)
                .<PaymentSettlementProjection, PaymentSettlementRow>chunk(500)
                .transactionManager(transactionManager)
                .reader(paymentSettlementReader)
                .processor(paymentSettlementItemProcessor)
                .writer(settlementDetailFileWriter)
                .build();
    }

    @Bean
    @StepScope
    public ItemStreamReader<PaymentSettlementProjection> paymentSettlementReader(
            EntityManagerFactory entityManagerFactory,
            @Value("#{jobParameters['targetDateFrom']}") String from,
            @Value("#{jobParameters['targetDateTo']}") String to) {
        return new PaymentSettlementKeysetItemReader(
                entityManagerFactory, Instant.parse(from), Instant.parse(to));
    }

    @Bean
    @StepScope
    public PaymentSettlementItemProcessor paymentSettlementItemProcessor(
            @Value("#{jobParameters['targetDateFrom']}") String from) {
        return new PaymentSettlementItemProcessor(targetDateYyyyMMdd(from));
    }

    @Bean
    @StepScope
    public FlatFileItemWriter<PaymentSettlementRow> settlementDetailFileWriter(
            @Value("${batch.output.dir}") String outputDir,
            @Value("#{jobParameters['targetDateFrom']}") String from) {
        String targetDate = targetDateYyyyMMdd(from);
        Path outputDirPath = Paths.get(outputDir);
        try {
            Files.createDirectories(outputDirPath);
        } catch (IOException e) {
            throw new UncheckedIOException("出力ディレクトリの作成に失敗しました: " + outputDirPath, e);
        }
        Path csv = outputDirPath.resolve("settlement_detail_" + targetDate + ".csv");
        return new FlatFileItemWriterBuilder<PaymentSettlementRow>()
                .name("settlementDetailFileWriter")
                .resource(new FileSystemResource(csv))
                .headerCallback(
                        writer ->
                                writer.write(
                                        "order_id,payment_id,captured_at,amount,fee,net_amount,settlement_cycle"))
                .lineAggregator(this::toSettlementCsvLine)
                .build();
    }

    private String toSettlementCsvLine(PaymentSettlementRow row) {
        return String.join(
                ",",
                String.valueOf(row.orderId()),
                row.paymentId(),
                row.capturedAt().toString(),
                row.amount().toPlainString(),
                row.fee().toPlainString(),
                row.netAmount().toPlainString(),
                row.settlementCycle());
    }

    /**
     * settlementDetailExportStepとは別Stepにすることで、CSV生成（重いI/O）だけをやり直さず
     * フラグ生成のみリスタートできるようにする（14.4節「外部I/OとDB内部処理を同じStepに混在させない」と 同じ分割原則を送信フェーズ内部でも踏襲）。
     */
    @Bean
    public Step settlementDetailFlagExportStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            @Value("${batch.output.dir}") String outputDir) {
        Tasklet writeSettlementDetailFlagTasklet =
                (contribution, chunkContext) -> {
                    String targetDate =
                            targetDateYyyyMMdd(
                                    chunkContext
                                            .getStepContext()
                                            .getJobParameters()
                                            .get("targetDateFrom"));
                    writeCompletionFlag(Paths.get(outputDir), "settlement_detail_" + targetDate);
                    return RepeatStatus.FINISHED;
                };
        return new StepBuilder("settlementDetailFlagExportStep", jobRepository)
                .tasklet(writeSettlementDetailFlagTasklet, transactionManager)
                .build();
    }

    @Bean
    public Step completionFlagExportStep(
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
        return new StepBuilder("completionFlagExportStep", jobRepository)
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
