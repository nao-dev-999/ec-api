package com.example.ecapi.batch.job.paymentintake;

import com.example.ecapi.batch.dto.PaymentConfirmationRow;
import java.sql.Timestamp;
import javax.sql.DataSource;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.database.JdbcBatchItemWriter;
import org.springframework.batch.infrastructure.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

/**
 * paymentConfirmationIntakeStepが決済確定明細CSVを{@code payment_confirmation_staging}へ単純INSERTするWriter設定。
 *
 * <p>{@code settled_at}はpgjdbcが{@link java.time.Instant}を直接扱えない（型推論できずSQL例外になる）ため、{@link
 * Timestamp#from(java.time.Instant)}で変換してから渡す。
 */
@Configuration
public class PaymentConfirmationWriterConfig {

    private static final String STAGING_INSERT_SQL =
            """
            INSERT INTO payment_confirmation_staging
                (job_instance_id, order_number, transaction_id, customer_id, payment_method, status, amount, fee, settled_at)
            VALUES (:jobInstanceId, :orderNumber, :transactionId, :customerId, :paymentMethod, :status, :amount, :fee, :settledAt)
            """;

    @Bean
    @StepScope
    public JdbcBatchItemWriter<PaymentConfirmationRow> paymentConfirmationStagingWriter(
            DataSource dataSource,
            @Value("#{stepExecution.jobExecution.jobInstanceId}") Long jobInstanceId) {
        return new JdbcBatchItemWriterBuilder<PaymentConfirmationRow>()
                .dataSource(dataSource)
                .sql(STAGING_INSERT_SQL)
                .itemSqlParameterSourceProvider(
                        row -> {
                            var params = new MapSqlParameterSource();
                            params.addValue("jobInstanceId", jobInstanceId);
                            params.addValue("orderNumber", row.orderNumber());
                            params.addValue("transactionId", row.transactionId());
                            params.addValue("customerId", row.customerId());
                            params.addValue("paymentMethod", row.paymentMethod());
                            params.addValue("status", row.status());
                            params.addValue("amount", row.amount());
                            params.addValue("fee", row.fee());
                            params.addValue("settledAt", Timestamp.from(row.settledAt()));
                            return params;
                        })
                .build();
    }
}
