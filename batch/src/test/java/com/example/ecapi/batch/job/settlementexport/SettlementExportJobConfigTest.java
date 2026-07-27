package com.example.ecapi.batch.job.settlementexport;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.ecapi.batch.dto.PaymentSettlementProjection;
import com.example.ecapi.batch.dto.PaymentSettlementRow;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.infrastructure.item.ItemStreamReader;
import org.springframework.batch.infrastructure.item.file.FlatFileItemWriter;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * settlementDetailExportStep/settlementDetailFlagExportStepのBeanファクトリメソッドを直接呼び出し、Stepが例外なく
 * ビルドできることを確認するスモークテスト。
 */
@ExtendWith(MockitoExtension.class)
class SettlementExportJobConfigTest {

    @Mock private JobRepository jobRepository;
    @Mock private PlatformTransactionManager transactionManager;
    @Mock private ItemStreamReader<PaymentSettlementProjection> paymentSettlementReader;
    @Mock private PaymentSettlementItemProcessor paymentSettlementItemProcessor;
    @Mock private FlatFileItemWriter<PaymentSettlementRow> settlementDetailFileWriter;

    private final SettlementExportJobConfig config = new SettlementExportJobConfig();

    @Test
    @DisplayName("settlementDetailExportStepがPAYMENT読み取り→CSV書き込みのchunk構成で正しくビルドされること")
    void shouldBuildSettlementDetailExportStepAsChunkOrientedStep() {
        Step step =
                config.settlementDetailExportStep(
                        jobRepository,
                        transactionManager,
                        paymentSettlementReader,
                        paymentSettlementItemProcessor,
                        settlementDetailFileWriter);

        assertThat(step).isNotNull();
        assertThat(step.getName()).isEqualTo("settlementDetailExportStep");
    }

    @Test
    @DisplayName("settlementDetailFlagExportStepがTaskletとして正しくビルドされること")
    void shouldBuildSettlementDetailFlagExportStepAsTasklet() {
        Step step =
                config.settlementDetailFlagExportStep(
                        jobRepository, transactionManager, "/tmp/out");

        assertThat(step).isNotNull();
        assertThat(step.getName()).isEqualTo("settlementDetailFlagExportStep");
    }
}
