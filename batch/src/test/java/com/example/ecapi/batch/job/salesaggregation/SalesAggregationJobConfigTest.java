package com.example.ecapi.batch.job.salesaggregation;

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
 * salesAggregateWorkerStep/salesSummaryConsolidateStepのBeanファクトリメソッドを直接呼び出し、Stepが例外なく
 * ビルドできることを確認するスモークテスト。
 */
@ExtendWith(MockitoExtension.class)
class SalesAggregationJobConfigTest {

    @Mock private JobRepository jobRepository;
    @Mock private PlatformTransactionManager transactionManager;
    @Mock private ItemStreamReader<OrderDetailProjection> orderDetailReader;
    @Mock private SalesSummaryItemProcessor salesSummaryProcessor;
    @Mock private JdbcBatchItemWriter<SalesSummaryRow> salesSummaryStagingWriter;
    @Mock private StagingAggregateItemReader stagingAggregateItemReader;
    @Mock private JdbcBatchItemWriter<AggregatedSalesRow> dailySalesSummaryUpsertWriter;
    @Mock private StagingCleanupListener stagingCleanupListener;

    private final SalesAggregationJobConfig config = new SalesAggregationJobConfig();

    @Test
    @DisplayName("salesAggregateWorkerStepがskipなし・retryのみのfault tolerance構成で正しくビルドされること")
    void shouldBuildSalesAggregateWorkerStepWithRetryOnlyFaultTolerance() {
        Step step =
                config.salesAggregateWorkerStep(
                        jobRepository,
                        transactionManager,
                        orderDetailReader,
                        salesSummaryProcessor,
                        salesSummaryStagingWriter);

        assertThat(step).isNotNull();
        assertThat(step.getName()).isEqualTo("salesAggregateWorkerStep");
    }

    @Test
    @DisplayName("salesSummaryConsolidateStepがchunk構成で正しくビルドされること")
    void shouldBuildSalesSummaryConsolidateStepAsChunkOrientedStep() {
        Step step =
                config.salesSummaryConsolidateStep(
                        jobRepository,
                        transactionManager,
                        stagingAggregateItemReader,
                        dailySalesSummaryUpsertWriter,
                        stagingCleanupListener);

        assertThat(step).isNotNull();
        assertThat(step.getName()).isEqualTo("salesSummaryConsolidateStep");
    }
}
