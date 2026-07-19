package com.example.ecapi.batch.writer;

import com.example.ecapi.batch.config.BatchAuditConfig;
import com.example.ecapi.batch.dto.SalesSummaryRow;
import javax.sql.DataSource;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.database.JdbcBatchItemWriter;
import org.springframework.batch.infrastructure.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

/**
 * UPSERT はJPAの save() でネイティブ表現できないため、素のJDBCで書く（14.7節④参照）。
 * 複数パーティションが同一商品を並行して書き込みうるため、REPLACEではなくADD（積算）で 部分集計を正しく合算する。
 */
@Configuration
public class SalesSummaryWriterConfig {

    private static final String UPSERT_SQL =
            """
            INSERT INTO daily_sales_summary_by_product
                (product_id, sales_date, total_amount, total_quantity,
                 version, created_by, updated_by, created_at, updated_at)
            VALUES (:productId, :salesDate, :amount, :quantity,
                    0, :systemUserId, :systemUserId, now(), now())
            ON CONFLICT (product_id, sales_date)
            DO UPDATE SET
                total_amount = daily_sales_summary_by_product.total_amount + EXCLUDED.total_amount,
                total_quantity = daily_sales_summary_by_product.total_quantity + EXCLUDED.total_quantity,
                version = daily_sales_summary_by_product.version + 1,
                updated_by = :systemUserId,
                updated_at = now()
            """;

    @Bean
    @StepScope
    public JdbcBatchItemWriter<SalesSummaryRow> salesSummaryWriter(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<SalesSummaryRow>()
                .dataSource(dataSource)
                .sql(UPSERT_SQL)
                .itemSqlParameterSourceProvider(
                        row -> {
                            var params = new MapSqlParameterSource();
                            params.addValue("productId", row.productId());
                            params.addValue("salesDate", row.salesDate());
                            params.addValue("amount", row.amount());
                            params.addValue("quantity", row.quantity());
                            params.addValue("systemUserId", BatchAuditConfig.BATCH_SYSTEM_USER_ID);
                            return params;
                        })
                .build();
    }
}
