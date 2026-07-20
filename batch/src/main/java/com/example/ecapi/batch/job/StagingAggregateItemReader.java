package com.example.ecapi.batch.job;

import com.example.ecapi.batch.dto.AggregatedSalesRow;
import java.time.LocalDate;
import javax.sql.DataSource;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemStreamException;
import org.springframework.batch.infrastructure.item.ItemStreamReader;
import org.springframework.batch.infrastructure.item.database.JdbcCursorItemReader;
import org.springframework.batch.infrastructure.item.database.builder.JdbcCursorItemReaderBuilder;

/**
 * daily_sales_summary_stagingを{@code job_instance_id}で絞り込んでGROUP BY/SUMした結果を
 * カーソルで1行ずつ返す（全件を一度にメモリへ載せない）。対象はstagingの集約結果であり 失敗時に同じ{@code
 * job_instance_id}の全データを毎回読み直すコストは小さいため、 {@link OrderDetailKeysetItemReader}とは異なりリスタート時の位置復元は行わない
 * （{@code saveState(false)}）。カーソル自体の実装はSpring Batch標準の {@link JdbcCursorItemReader}に委譲する。
 */
public class StagingAggregateItemReader implements ItemStreamReader<AggregatedSalesRow> {

    private static final String QUERY =
            """
            SELECT product_id, sales_date, SUM(amount) AS total_amount, SUM(quantity) AS total_quantity
            FROM daily_sales_summary_staging
            WHERE job_instance_id = ?
            GROUP BY product_id, sales_date
            """;

    private static final int FETCH_SIZE = 500;

    private final JdbcCursorItemReader<AggregatedSalesRow> delegate;

    public StagingAggregateItemReader(DataSource dataSource, long jobInstanceId) {
        this.delegate =
                new JdbcCursorItemReaderBuilder<AggregatedSalesRow>()
                        .name("stagingAggregateItemReader")
                        .dataSource(dataSource)
                        .sql(QUERY)
                        .queryArguments(jobInstanceId)
                        .fetchSize(FETCH_SIZE)
                        .saveState(false)
                        .rowMapper(
                                (rs, rowNum) ->
                                        new AggregatedSalesRow(
                                                rs.getLong("product_id"),
                                                rs.getObject("sales_date", LocalDate.class),
                                                rs.getBigDecimal("total_amount"),
                                                rs.getInt("total_quantity")))
                        .build();
    }

    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        delegate.open(executionContext);
    }

    @Override
    public AggregatedSalesRow read() throws Exception {
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
