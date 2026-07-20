package com.example.ecapi.batch.job;

import java.util.Map;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.listener.StepExecutionListener;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/**
 * jobBConsolidateStep完了後にステージング行をまとめてDELETEする。
 *
 * <p>chunk処理中に都度DELETEすると{@link StagingAggregateItemReader}のGROUP BY結果が
 * 途中で変わってしまうため、全chunk完了後の一括DELETEにする。Step失敗時はDELETEしない
 * （このStepはリスタート時に位置復元をせず毎回最初から読み直す設計のため、ステージング行を 残しておけば安全に再実行できる）。
 */
public class StagingCleanupListener implements StepExecutionListener {

    private static final String DELETE_STAGING_SQL =
            "DELETE FROM daily_sales_summary_staging WHERE job_instance_id = :jobInstanceId";

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public StagingCleanupListener(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        if (stepExecution.getStatus() != BatchStatus.COMPLETED) {
            return null;
        }
        long jobInstanceId = stepExecution.getJobExecution().getJobInstanceId();
        jdbcTemplate.update(DELETE_STAGING_SQL, Map.of("jobInstanceId", jobInstanceId));
        return null;
    }
}
