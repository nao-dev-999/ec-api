package com.example.ecapi.batch.job.settlementexport;

import com.example.ecapi.batch.dto.PaymentSettlementProjection;
import com.example.ecapi.batch.dto.PaymentSettlementRow;
import com.example.ecapi.batch.job.dailysales.TargetDateFormatter;
import jakarta.persistence.EntityManagerFactory;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.item.ItemStreamReader;
import org.springframework.batch.infrastructure.item.file.FlatFileItemWriter;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * 決済明細出力ジョブ（旧dailySalesAggregationJobの送信フェーズ）。 PAYMENTテーブル（status =
 * CAPTURED）から決済明細CSV（入金消込用）を直接抽出・出力し、 決済明細専用の完了フラグ、続いてジョブネット全体の完了を示すフラグを生成する。
 * completionFlagExportStepはジョブネット全体の完了を示すフラグのため、 3ジョブのうち最後に実行される想定のこのJobの末尾に置く。
 */
@Configuration
public class SettlementExportJobConfig {

    @Bean
    public Job settlementExportJob(
            JobRepository jobRepository,
            Step settlementDetailExportStep,
            Step settlementDetailFlagExportStep,
            Step completionFlagExportStep) {
        return new JobBuilder("settlementExportJob", jobRepository)
                .start(settlementDetailExportStep)
                .next(settlementDetailFlagExportStep)
                .next(completionFlagExportStep)
                .build();
    }

    /**
     * 決済システム（自社の別システム、入金消込用）向けの決済明細ファイルを生成するStep。
     * daily_sales_summary_by_product（商品単位の集計値。payment_id・fee・net_amountを持たない）は経由せず、
     * PAYMENTテーブル（status = CAPTURED）から直接抽出する。1オーダー1決済のためPAYMENTの対象日分の件数規模は
     * CustomerOrderと同程度（ピーク日想定で最大20万件）となりうるため、salesAggregateWorkerStepと
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
        return new PaymentSettlementItemProcessor(TargetDateFormatter.yyyyMMdd(from));
    }

    @Bean
    @StepScope
    public FlatFileItemWriter<PaymentSettlementRow> settlementDetailFileWriter(
            @Value("${batch.output.dir}") String outputDir,
            @Value("#{jobParameters['targetDateFrom']}") String from) {
        String targetDate = TargetDateFormatter.yyyyMMdd(from);
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
     * フラグ生成のみリスタートできるようにする（「外部I/OとDB内部処理を同じStepに混在させない」と 同じ分割原則を送信フェーズ内部でも踏襲）。
     */
    @Bean
    public Step settlementDetailFlagExportStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            @Value("${batch.output.dir}") String outputDir) {
        Tasklet writeSettlementDetailFlagTasklet =
                (contribution, chunkContext) -> {
                    String targetDate =
                            TargetDateFormatter.yyyyMMdd(
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
}
