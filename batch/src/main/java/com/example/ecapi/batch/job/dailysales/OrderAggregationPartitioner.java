package com.example.ecapi.batch.job.dailysales;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.springframework.batch.core.partition.Partitioner;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

public class OrderAggregationPartitioner implements Partitioner {

    private static final String ID_RANGE_SQL =
            """
            SELECT COALESCE(MIN(customer_order_id), 0), COALESCE(MAX(customer_order_id), 0)
            FROM customer_order_detail
            WHERE created_at BETWEEN :from AND :to
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
                new MapSqlParameterSource().addValue("from", from).addValue("to", to);
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
