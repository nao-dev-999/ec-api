package com.example.ecapi.batch.job.paymentintake;

import com.example.ecapi.batch.dto.PaymentReconciliationRow;
import java.sql.Timestamp;
import javax.sql.DataSource;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemStreamException;
import org.springframework.batch.infrastructure.item.ItemStreamReader;
import org.springframework.batch.infrastructure.item.database.JdbcCursorItemReader;
import org.springframework.batch.infrastructure.item.database.builder.JdbcCursorItemReaderBuilder;

/**
 * payment_confirmation_stagingをcustomer_orderへLEFT JOINし、{@code job_instance_id}で絞り込んだ結果を
 * カーソルで1行ずつ返す。孤立レコード（customer_order_idがNULL）の判定もこのJOINで行い、Processor側で 1行ずつ突合クエリを発行するN+1を避ける。
 *
 * <p>{@link
 * com.example.ecapi.batch.job.salesaggregation.StagingAggregateItemReader}と同様、対象はステージングの内容そのものであり
 * ステージング行はStep失敗後も削除されない（V13/V15の方針どおり監査目的で保持し続ける）ため、 リスタート時の位置復元は行わない（{@code saveState(false)}）。
 */
public class PaymentReconciliationItemReader implements ItemStreamReader<PaymentReconciliationRow> {

    private static final String QUERY =
            """
            SELECT o.id AS customer_order_id, o.ordered_at AS ordered_at,
                   s.order_number, s.transaction_id, s.status, s.amount, s.fee, s.settled_at
            FROM payment_confirmation_staging s
            LEFT JOIN customer_order o ON o.order_number = s.order_number
            WHERE s.job_instance_id = ?
            """;

    private final JdbcCursorItemReader<PaymentReconciliationRow> delegate;

    public PaymentReconciliationItemReader(DataSource dataSource, long jobInstanceId) {
        this.delegate =
                new JdbcCursorItemReaderBuilder<PaymentReconciliationRow>()
                        .name("paymentReconciliationItemReader")
                        .dataSource(dataSource)
                        .sql(QUERY)
                        .queryArguments(jobInstanceId)
                        .saveState(false)
                        .rowMapper(
                                (rs, rowNum) -> {
                                    Timestamp orderedAt = rs.getTimestamp("ordered_at");
                                    return new PaymentReconciliationRow(
                                            (Long) rs.getObject("customer_order_id"),
                                            orderedAt == null ? null : orderedAt.toInstant(),
                                            rs.getString("order_number"),
                                            rs.getString("transaction_id"),
                                            rs.getString("status"),
                                            rs.getBigDecimal("amount"),
                                            rs.getBigDecimal("fee"),
                                            rs.getTimestamp("settled_at").toInstant());
                                })
                        .build();
    }

    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        delegate.open(executionContext);
    }

    @Override
    public PaymentReconciliationRow read() throws Exception {
        return delegate.read();
    }

    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException {
        // saveState(false)によりリスタート位置は保存しない（毎回最初から読み直す設計のため）
    }

    @Override
    public void close() throws ItemStreamException {
        delegate.close();
    }
}
