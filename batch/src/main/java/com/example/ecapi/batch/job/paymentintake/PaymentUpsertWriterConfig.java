package com.example.ecapi.batch.job.paymentintake;

import com.example.ecapi.batch.config.BatchAuditConfig;
import com.example.ecapi.batch.dto.PaymentUpsertRow;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.batch.infrastructure.item.database.JdbcBatchItemWriter;
import org.springframework.batch.infrastructure.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.infrastructure.item.support.CompositeItemWriter;
import org.springframework.batch.infrastructure.item.support.builder.CompositeItemWriterBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

/**
 * paymentReconciliationStepの反映先。1件のPaymentUpsertRowにつき ①paymentへのUPSERT
 * ②customer_order.payment_statusの更新、の2つのSQLを実行する必要があるため、 それぞれ単責務のJdbcBatchItemWriterに分け{@link
 * CompositeItemWriter}で束ねる（ステージング+置き換え方式をpaymentテーブルにも踏襲。積算ではなくEXCLUDED値でのそのまま置換）。
 *
 * <p>authorized_atはON CONFLICTのDO UPDATE SETに含めない。初回INSERT時の値 （customer_order.ordered_atの近似値、{@code
 * PaymentReconciliationItemProcessor}参照）を 突合再実行後も保持するため。
 *
 * <p>captured_atはCOALESCE(EXCLUDED.captured_at, payment.captured_at)で置換する。{@code
 * PaymentReconciliationItemProcessor}はstatus=CAPTUREDのときのみcaptured_atを設定しそれ以外はnullを渡すため、
 * CAPTURED後に届くREFUNDED/FAILEDのイベントで実際の決済確定日時が上書き消失するのを防ぐ。
 */
@Configuration
public class PaymentUpsertWriterConfig {

    private static final String PAYMENT_UPSERT_SQL =
            """
            INSERT INTO payment
                (customer_order_id, transaction_id, status, amount, fee, net_amount,
                 authorized_at, captured_at, version, created_by, updated_by, created_at, updated_at)
            VALUES
                (:customerOrderId, :transactionId, :status, :amount, :fee, :netAmount,
                 :authorizedAt, :capturedAt, 0, :systemUserId, :systemUserId, now(), now())
            ON CONFLICT (customer_order_id)
            DO UPDATE SET
                transaction_id = EXCLUDED.transaction_id,
                status = EXCLUDED.status,
                amount = EXCLUDED.amount,
                fee = EXCLUDED.fee,
                net_amount = EXCLUDED.net_amount,
                captured_at = COALESCE(EXCLUDED.captured_at, payment.captured_at),
                version = payment.version + 1,
                updated_by = :systemUserId,
                updated_at = now()
            """;

    private static final String ORDER_PAYMENT_STATUS_UPDATE_SQL =
            """
            UPDATE customer_order
            SET payment_status = :orderPaymentStatus,
                version = version + 1,
                updated_by = :systemUserId,
                updated_at = now()
            WHERE id = :customerOrderId
            """;

    @Bean
    public JdbcBatchItemWriter<PaymentUpsertRow> paymentUpsertWriter(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<PaymentUpsertRow>()
                .dataSource(dataSource)
                .sql(PAYMENT_UPSERT_SQL)
                .itemSqlParameterSourceProvider(
                        row -> {
                            var params = new MapSqlParameterSource();
                            params.addValue("customerOrderId", row.customerOrderId());
                            params.addValue("transactionId", row.transactionId());
                            params.addValue("status", row.status().name());
                            params.addValue("amount", row.amount());
                            params.addValue("fee", row.fee());
                            params.addValue("netAmount", row.netAmount());
                            params.addValue("authorizedAt", Timestamp.from(row.authorizedAt()));
                            params.addValue(
                                    "capturedAt",
                                    row.capturedAt() == null
                                            ? null
                                            : Timestamp.from(row.capturedAt()),
                                    Types.TIMESTAMP);
                            params.addValue("systemUserId", BatchAuditConfig.BATCH_SYSTEM_USER_ID);
                            return params;
                        })
                .build();
    }

    @Bean
    public JdbcBatchItemWriter<PaymentUpsertRow> customerOrderPaymentStatusWriter(
            DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<PaymentUpsertRow>()
                .dataSource(dataSource)
                .sql(ORDER_PAYMENT_STATUS_UPDATE_SQL)
                .itemSqlParameterSourceProvider(
                        row -> {
                            var params = new MapSqlParameterSource();
                            params.addValue("customerOrderId", row.customerOrderId());
                            params.addValue("orderPaymentStatus", row.orderPaymentStatus().name());
                            params.addValue("systemUserId", BatchAuditConfig.BATCH_SYSTEM_USER_ID);
                            return params;
                        })
                .build();
    }

    @Bean
    public CompositeItemWriter<PaymentUpsertRow> paymentReconciliationWriter(
            JdbcBatchItemWriter<PaymentUpsertRow> paymentUpsertWriter,
            JdbcBatchItemWriter<PaymentUpsertRow> customerOrderPaymentStatusWriter) {
        return new CompositeItemWriterBuilder<PaymentUpsertRow>()
                .delegates(List.of(paymentUpsertWriter, customerOrderPaymentStatusWriter))
                .build();
    }
}
