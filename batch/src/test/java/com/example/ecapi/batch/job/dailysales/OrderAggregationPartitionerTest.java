package com.example.ecapi.batch.job.dailysales;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@ExtendWith(MockitoExtension.class)
class OrderAggregationPartitionerTest {

    @Mock private NamedParameterJdbcTemplate jdbcTemplate;
    @Mock private JdbcTemplate plainJdbcTemplate;

    @Test
    @DisplayName("最大IDをgridSizeで均等に分割してレンジ全体を過不足なくカバーすること")
    void shouldSplitMaxIdEvenlyAcrossGridSize() {
        when(jdbcTemplate.getJdbcTemplate()).thenReturn(plainJdbcTemplate);
        when(plainJdbcTemplate.queryForObject(
                        "SELECT COALESCE(MAX(id), 0) FROM customer_order", Long.class))
                .thenReturn(1000L);
        OrderAggregationPartitioner partitioner = new OrderAggregationPartitioner(jdbcTemplate);

        Map<String, ExecutionContext> partitions = partitioner.partition(4);

        assertThat(partitions).hasSize(4);
        assertThat(partitions.get("partition0").getLong("minId")).isEqualTo(0L);
        assertThat(partitions.get("partition0").getLong("maxId")).isEqualTo(250L);
        assertThat(partitions.get("partition3").getLong("minId")).isEqualTo(750L);
        assertThat(partitions.get("partition3").getLong("maxId")).isEqualTo(1000L);
    }

    @Test
    @DisplayName("注文が0件でも例外を投げず全パーティションがID0近辺を指すこと")
    void shouldNotThrowWhenNoOrdersExist() {
        when(jdbcTemplate.getJdbcTemplate()).thenReturn(plainJdbcTemplate);
        when(plainJdbcTemplate.queryForObject(
                        "SELECT COALESCE(MAX(id), 0) FROM customer_order", Long.class))
                .thenReturn(0L);
        OrderAggregationPartitioner partitioner = new OrderAggregationPartitioner(jdbcTemplate);

        Map<String, ExecutionContext> partitions = partitioner.partition(4);

        assertThat(partitions).hasSize(4);
        partitions
                .values()
                .forEach(
                        ctx -> {
                            assertThat(ctx.getLong("minId")).isBetween(0L, 4L);
                            assertThat(ctx.getLong("maxId")).isBetween(0L, 4L);
                        });
    }
}
