package com.example.ecapi.batch.job.salesaggregation;

import com.example.ecapi.constant.PaymentStatus;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.springframework.batch.core.partition.Partitioner;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/**
 * salesAggregateWorkerStep（{@link
 * com.example.ecapi.batch.reader.OrderDetailKeysetItemReader}相当のクエリ）が
 * 対象にする注文明細のcustomer_order_id範囲を求め、gridSize個のパーティションへ均等分割する。
 *
 * <p>Readerの実際の抽出条件（{@code PAYMENT.status = CAPTURED} かつ{@code PAYMENT.captured_at}が対象日時範囲内）と
 * 一致させる必要がある。ここが{@code customer_order_detail.created_at}基準のままだと、created_atが対象日時範囲外
 * （例えば前日以前に作成されたが対象日にcaptured_atとなった注文）のIDがレンジから漏れ、Readerの`BETWEEN :minId AND
 * :maxId`条件によって黙って集計対象から除外されてしまう。
 */
public class OrderAggregationPartitioner implements Partitioner {

    private static final String ID_RANGE_SQL =
            """
            SELECT COALESCE(MIN(d.customer_order_id), 0), COALESCE(MAX(d.customer_order_id), 0)
            FROM customer_order_detail d
            JOIN payment p ON p.customer_order_id = d.customer_order_id
            WHERE p.status = :status
              AND p.captured_at BETWEEN :from AND :to
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final Instant from;
    private final Instant to;

    public OrderAggregationPartitioner(
            NamedParameterJdbcTemplate jdbcTemplate, Instant from, Instant to) {
        this.jdbcTemplate = jdbcTemplate;
        this.from = from;
        this.to = to;
    }

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        MapSqlParameterSource params =
                new MapSqlParameterSource()
                        .addValue("status", PaymentStatus.CAPTURED.name())
                        .addValue("from", from)
                        .addValue("to", to);
        long[] idRange =
                jdbcTemplate.queryForObject(
                        ID_RANGE_SQL, params, (rs, i) -> new long[] {rs.getLong(1), rs.getLong(2)});
        long minId = idRange[0];
        long maxId = idRange[1];
        long rangeSize = Math.max((maxId - minId) / gridSize, 1);

        Map<String, ExecutionContext> partitions = new HashMap<>();
        for (int i = 0; i < gridSize; i++) {
            ExecutionContext ctx = new ExecutionContext();
            ctx.putLong("minId", i == 0 ? minId : minId + i * rangeSize + 1);
            ctx.putLong("maxId", i == gridSize - 1 ? maxId : minId + (i + 1) * rangeSize);
            partitions.put("partition" + i, ctx);
        }
        return partitions;
    }
}
