package com.example.ecapi.batch.job.salesaggregation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

@ExtendWith(MockitoExtension.class)
class OrderAggregationPartitionerTest {

    private static final Instant FROM = Instant.parse("2026-07-25T15:00:00Z");
    private static final Instant TO = Instant.parse("2026-07-26T15:00:00Z");

    @Mock private NamedParameterJdbcTemplate jdbcTemplate;

    @SuppressWarnings("unchecked")
    private void stubIdRange(long minId, long maxId) {
        when(jdbcTemplate.queryForObject(
                        any(String.class), any(SqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(new long[] {minId, maxId});
    }

    @Test
    @DisplayName("対象日のID範囲をgridSizeで均等に分割してレンジ全体を過不足なくカバーすること")
    void shouldSplitIdRangeEvenlyAcrossGridSize() {
        stubIdRange(0L, 1000L);
        OrderAggregationPartitioner partitioner =
                new OrderAggregationPartitioner(jdbcTemplate, FROM, TO);

        Map<String, ExecutionContext> partitions = partitioner.partition(4);

        assertThat(partitions).hasSize(4);
        assertThat(partitions.get("partition0").getLong("minId")).isEqualTo(0L);
        assertThat(partitions.get("partition0").getLong("maxId")).isEqualTo(250L);
        assertThat(partitions.get("partition1").getLong("minId")).isEqualTo(251L);
        assertThat(partitions.get("partition3").getLong("minId")).isEqualTo(751L);
        assertThat(partitions.get("partition3").getLong("maxId")).isEqualTo(1000L);
    }

    @Test
    @DisplayName("対象日のIDがテーブル全体の末尾に偏っていても、そのレンジ内で均等分割されること")
    void shouldOffsetFromMinIdWhenTargetDayIdsAreNotZeroBased() {
        stubIdRange(900_000L, 901_000L);
        OrderAggregationPartitioner partitioner =
                new OrderAggregationPartitioner(jdbcTemplate, FROM, TO);

        Map<String, ExecutionContext> partitions = partitioner.partition(4);

        assertThat(partitions.get("partition0").getLong("minId")).isEqualTo(900_000L);
        assertThat(partitions.get("partition0").getLong("maxId")).isEqualTo(900_250L);
        assertThat(partitions.get("partition3").getLong("minId")).isEqualTo(900_751L);
        assertThat(partitions.get("partition3").getLong("maxId")).isEqualTo(901_000L);
    }

    @Test
    @DisplayName("BETWEENで読むリーダーとの整合上、隣接パーティションの範囲が重複しないこと")
    void shouldNotOverlapAdjacentPartitionRanges() {
        stubIdRange(0L, 1000L);
        OrderAggregationPartitioner partitioner =
                new OrderAggregationPartitioner(jdbcTemplate, FROM, TO);

        Map<String, ExecutionContext> partitions = partitioner.partition(4);

        for (int i = 0; i < 3; i++) {
            long currentMaxId = partitions.get("partition" + i).getLong("maxId");
            long nextMinId = partitions.get("partition" + (i + 1)).getLong("minId");
            assertThat(nextMinId)
                    .as("partition%d のmaxIdとpartition%d のminIdはBETWEEN境界で重複してはならない", i, i + 1)
                    .isEqualTo(currentMaxId + 1);
        }
    }

    @Test
    @DisplayName("対象日の注文が0件でも例外を投げず全パーティションがID0近辺を指すこと")
    void shouldNotThrowWhenNoOrdersExist() {
        stubIdRange(0L, 0L);
        OrderAggregationPartitioner partitioner =
                new OrderAggregationPartitioner(jdbcTemplate, FROM, TO);

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
