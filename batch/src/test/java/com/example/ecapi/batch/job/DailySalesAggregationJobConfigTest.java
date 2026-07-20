package com.example.ecapi.batch.job;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.ecapi.batch.dto.AggregatedSalesRow;
import com.example.ecapi.batch.dto.OrderDetailProjection;
import com.example.ecapi.batch.dto.SalesSummaryRow;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.infrastructure.item.ItemStreamReader;
import org.springframework.batch.infrastructure.item.database.JdbcBatchItemWriter;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * jobBWorkerStep/jobBConsolidateStepのBeanファクトリメソッドを直接呼び出し、Stepが例外なく ビルドできることを確認するスモークテスト（{@link
 * BatchFaultTolerancePolicy}適用込み）。
 */
@ExtendWith(MockitoExtension.class)
class DailySalesAggregationJobConfigTest {

    @Mock private JobRepository jobRepository;
    @Mock private PlatformTransactionManager transactionManager;
    @Mock private ItemStreamReader<OrderDetailProjection> orderDetailReader;
    @Mock private SalesSummaryItemProcessor salesSummaryProcessor;
    @Mock private JdbcBatchItemWriter<SalesSummaryRow> salesSummaryStagingWriter;
    @Mock private SalesSummarySkipListener salesSummarySkipListener;
    @Mock private StagingAggregateItemReader stagingAggregateItemReader;
    @Mock private JdbcBatchItemWriter<AggregatedSalesRow> dailySalesSummaryUpsertWriter;
    @Mock private StagingCleanupListener stagingCleanupListener;

    private final DailySalesAggregationJobConfig config = new DailySalesAggregationJobConfig();

    @Test
    @DisplayName("jobBWorkerStepがBatchFaultTolerancePolicy適用込みで正しくビルドされること")
    void shouldBuildJobBWorkerStepWithFaultTolerancePolicyApplied() {
        Step step =
                config.jobBWorkerStep(
                        jobRepository,
                        transactionManager,
                        orderDetailReader,
                        salesSummaryProcessor,
                        salesSummaryStagingWriter,
                        salesSummarySkipListener);

        assertThat(step).isNotNull();
        assertThat(step.getName()).isEqualTo("jobBWorkerStep");
    }

    @Test
    @DisplayName("jobBConsolidateStepがchunk構成で正しくビルドされること")
    void shouldBuildJobBConsolidateStepAsChunkOrientedStep() {
        Step step =
                config.jobBConsolidateStep(
                        jobRepository,
                        transactionManager,
                        stagingAggregateItemReader,
                        dailySalesSummaryUpsertWriter,
                        stagingCleanupListener);

        assertThat(step).isNotNull();
        assertThat(step.getName()).isEqualTo("jobBConsolidateStep");
    }
}
