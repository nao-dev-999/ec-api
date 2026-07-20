package com.example.ecapi.batch.job;

import com.example.ecapi.batch.dto.OrderDetailProjection;
import com.example.ecapi.batch.dto.SalesSummaryRow;
import java.util.Map;
import org.springframework.batch.core.listener.SkipListener;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/**
 * スキップされたレコードを{@code batch_skipped_records}に記録する（監査目的）。 Local
 * Partitioningでパーティション毎にThreadが分かれるため、{@code jobExecutionId}/{@code stepName}
 * は他のReader/Processor/Writer同様に{@code @StepScope}経由で注入する（インスタンスフィールドで
 * StepExecutionを保持すると、パーティション間で共有される場合に競合しうるため）。
 */
public class SalesSummarySkipListener
        implements SkipListener<OrderDetailProjection, SalesSummaryRow> {

    private static final String INSERT_SQL =
            """
            INSERT INTO batch_skipped_records
                (job_execution_id, step_name, order_detail_id, error_message)
            VALUES (:jobExecutionId, :stepName, :orderDetailId, :errorMessage)
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final Long jobExecutionId;
    private final String stepName;

    public SalesSummarySkipListener(
            NamedParameterJdbcTemplate jdbcTemplate, Long jobExecutionId, String stepName) {
        this.jdbcTemplate = jdbcTemplate;
        this.jobExecutionId = jobExecutionId;
        this.stepName = stepName;
    }

    @Override
    public void onSkipInProcess(OrderDetailProjection item, Throwable t) {
        Map<String, Object> params =
                Map.of(
                        "jobExecutionId",
                        jobExecutionId,
                        "stepName",
                        stepName,
                        "orderDetailId",
                        item.id(),
                        "errorMessage",
                        String.valueOf(t.getMessage()));
        jdbcTemplate.update(INSERT_SQL, params);
    }
}
