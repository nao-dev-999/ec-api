package com.example.ecapi.batch.job.dailysales;

import com.example.ecapi.batch.dto.OrderDetailProjection;
import com.example.ecapi.batch.dto.SalesSummaryRow;
import java.util.Map;
import org.springframework.batch.core.listener.SkipListener;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/**
 * fault toleranceでスキップされたレコードを{@code batch_skipped_records}に記録する（監査目的）。
 *
 * <p>{@code salesAggregatePartitionStep}はLocal Partitioningによりパーティションごとに別Threadで並列実行されるため、
 * 本リスナーもパーティション（Thread）ごとに専用インスタンスとして動作する必要がある。そのため{@code jobExecutionId}/{@code
 * stepName}は他のReader/Processorと同様に{@code @StepScope}経由でコンストラクタに注入し、 イミュータブルなフィールドとして保持する。仮に{@code
 * StepExecution}そのものをフィールドで保持し実行時に都度参照する 実装にすると、複数パーティションのThreadが同一の共有状態を参照してしまい競合が起こり得る。
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
