package com.example.ecapi.batch.writer;

import com.example.ecapi.batch.config.BatchAuditConfig;
import com.example.ecapi.batch.dto.AggregatedSalesRow;
import com.example.ecapi.batch.dto.SalesSummaryRow;
import javax.sql.DataSource;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.database.JdbcBatchItemWriter;
import org.springframework.batch.infrastructure.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

/**
 * Workerは最終テーブルへ直接書かず、ステージングテーブルへ明細のまま単純INSERTする（14.7節④・14.8節参照）。
 * 集約・最終テーブルへの反映はjobBConsolidateStep（{@code StagingAggregateItemReader} + {@code
 * dailySalesSummaryUpsertWriter}）が担う。
 */
@Configuration
public class SalesSummaryWriterConfig {

    private static final String STAGING_INSERT_SQL =
            """
            INSERT INTO daily_sales_summary_staging
                (job_instance_id, order_detail_id, product_id, sales_date, amount, quantity)
            VALUES (:jobInstanceId, :orderDetailId, :productId, :salesDate, :amount, :quantity)
            """;

    private static final String UPSERT_SQL =
            """
            INSERT INTO daily_sales_summary_by_product
                (product_id, sales_date, total_amount, total_quantity,
                 version, created_by, updated_by, created_at, updated_at)
            VALUES (:productId, :salesDate, :totalAmount, :totalQuantity,
                    0, :systemUserId, :systemUserId, now(), now())
            ON CONFLICT (product_id, sales_date)
            DO UPDATE SET
                total_amount = EXCLUDED.total_amount,
                total_quantity = EXCLUDED.total_quantity,
                version = daily_sales_summary_by_product.version + 1,
                updated_by = :systemUserId,
                updated_at = now()
            """;

    @Bean
    @StepScope
    public JdbcBatchItemWriter<SalesSummaryRow> salesSummaryStagingWriter(
            DataSource dataSource,
            @Value("#{stepExecution.jobExecution.jobInstanceId}") Long jobInstanceId) {
        return new JdbcBatchItemWriterBuilder<SalesSummaryRow>()
                .dataSource(dataSource)
                .sql(STAGING_INSERT_SQL)
                .itemSqlParameterSourceProvider(
                        row -> {
                            var params = new MapSqlParameterSource();
                            params.addValue("jobInstanceId", jobInstanceId);
                            params.addValue("orderDetailId", row.orderDetailId());
                            params.addValue("productId", row.productId());
                            params.addValue("salesDate", row.salesDate());
                            params.addValue("amount", row.amount());
                            params.addValue("quantity", row.quantity());
                            return params;
                        })
                .build();
    }

    @Bean
    public JdbcBatchItemWriter<AggregatedSalesRow> dailySalesSummaryUpsertWriter(
            DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<AggregatedSalesRow>()
                .dataSource(dataSource)
                .sql(UPSERT_SQL)
                .itemSqlParameterSourceProvider(
                        row -> {
                            var params = new MapSqlParameterSource();
                            params.addValue("productId", row.productId());
                            params.addValue("salesDate", row.salesDate());
                            params.addValue("totalAmount", row.totalAmount());
                            params.addValue("totalQuantity", row.totalQuantity());
                            params.addValue("systemUserId", BatchAuditConfig.BATCH_SYSTEM_USER_ID);
                            return params;
                        })
                .build();
    }
}
