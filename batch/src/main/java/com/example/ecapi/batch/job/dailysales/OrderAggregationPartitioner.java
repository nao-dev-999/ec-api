package com.example.ecapi.batch.job.dailysales;

import java.util.HashMap;
import java.util.Map;
import org.springframework.batch.core.partition.Partitioner;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/**
 * 顧客IDではなく CustomerOrder.id のレンジをパーティションキーとする（14.5節参照）。
 * 顧客IDだと注文の時間的発生ムラの影響を受けやすいため、ID範囲の方が均等な負荷分散になりやすい。
 *
 * <p>最大IDの取得はJob開始時に1度だけ行う軽量な集約クエリであり、200万件規模のRead/Writeとは異なりHibernateの
 * 永続化コンテキストを介する必要がないため、素のJDBCで発行する（14.7節①参照）。
 */
public class OrderAggregationPartitioner implements Partitioner {

    private static final String MAX_ID_SQL = "SELECT COALESCE(MAX(id), 0) FROM customer_order";

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public OrderAggregationPartitioner(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        long maxId = jdbcTemplate.getJdbcTemplate().queryForObject(MAX_ID_SQL, Long.class);
        long rangeSize = Math.max(maxId / gridSize, 1);

        Map<String, ExecutionContext> partitions = new HashMap<>();
        for (int i = 0; i < gridSize; i++) {
            ExecutionContext ctx = new ExecutionContext();
            // BETWEEN minId AND maxIdは両端を含むため、minIdを前パーティションのmaxIdと同じ値にすると
            // 境界のIDが2つのパーティションに二重に含まれ、二重集計・staging PK重複を引き起こす。
            ctx.putLong("minId", i == 0 ? 0 : i * rangeSize + 1);
            ctx.putLong("maxId", i == gridSize - 1 ? maxId : (i + 1) * rangeSize);
            partitions.put("partition" + i, ctx);
        }
        return partitions;
    }
}
