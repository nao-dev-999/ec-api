package com.example.ecapi.batch.job.paymentintake;

import com.example.ecapi.batch.dto.PaymentConfirmationRow;
import com.example.ecapi.batch.dto.PaymentReconciliationRow;
import com.example.ecapi.batch.dto.PaymentUpsertRow;
import com.example.ecapi.batch.exception.FlagFileNotFoundException;
import com.example.ecapi.batch.exception.InvalidPaymentConfirmationFormatException;
import com.example.ecapi.batch.exception.PaymentReconciliationException;
import com.example.ecapi.batch.job.dailysales.TargetDateFormatter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.infrastructure.item.support.CompositeItemWriter;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * 受信I/F取込ジョブ（旧dailySalesAggregationJobの取込フェーズ）。 受信フラグ確認→受信CSV検証・ステージング取込→customer_orderとの決済突合を行う。
 * 決済突合の結果（payment_reconciliation_alerts）に対するアラート判定は{@link
 * PaymentReconciliationAlertJobListener}がこのJob完了時に行う（他のJobには登録しない）。
 */
@Configuration
public class PaymentIntakeJobConfig {

    private static final String EXPECTED_CSV_HEADER =
            "order_number,transaction_id,customer_id,payment_method,status,amount,fee,settled_at";

    @Bean
    public Job paymentIntakeJob(
            JobRepository jobRepository,
            Step arrivalFlagCheckStep,
            Step paymentConfirmationIntakeStep,
            Step paymentReconciliationStep,
            PaymentReconciliationAlertJobListener paymentReconciliationAlertJobListener) {
        return new JobBuilder("paymentIntakeJob", jobRepository)
                .start(arrivalFlagCheckStep)
                .next(paymentConfirmationIntakeStep)
                .next(paymentReconciliationStep)
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
                            TargetDateFormatter.yyyyMMdd(
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
        String targetDate = TargetDateFormatter.yyyyMMdd(from);
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
}
