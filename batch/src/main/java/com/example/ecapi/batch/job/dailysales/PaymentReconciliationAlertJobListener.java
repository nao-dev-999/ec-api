package com.example.ecapi.batch.job.dailysales;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListener;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/**
 * Job完了後、当該{@code job_execution_id}に紐づく{@code payment_reconciliation_alerts}の件数を確認する。
 * 1件でも存在する場合（孤立レコード・未知のstatus等、人手調査が必要な行がある場合）、{@link
 * PaymentReconciliationSkipListener}によるDB記録だけでは検知漏れが起こりうるため、JobのExitStatusを{@value
 * #EXIT_STATUS_PARTIAL_SUCCESS_WITH_ALERTS}にして正常完了（COMPLETED）と区別できるようにする。
 *
 * <p>外部通知（Slack/メール等）は通知先未確定のため未実装。ExitStatusの区別とWARNログ出力までを行い、 最終的な終了コードへの変換（{@code
 * ExitCodeGenerator}によるOS終了コードの細分化）は別タスクとする。
 */
@Slf4j
public class PaymentReconciliationAlertJobListener implements JobExecutionListener {

    public static final String EXIT_STATUS_PARTIAL_SUCCESS_WITH_ALERTS =
            "PARTIAL_SUCCESS_WITH_ALERTS";

    private static final String COUNT_ALERTS_SQL =
            "SELECT COUNT(*) FROM payment_reconciliation_alerts WHERE job_execution_id = :jobExecutionId";

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public PaymentReconciliationAlertJobListener(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        if (jobExecution.getStatus() != BatchStatus.COMPLETED) {
            return;
        }

        Long jobExecutionId = jobExecution.getId();
        Long alertCount =
                jdbcTemplate.queryForObject(
                        COUNT_ALERTS_SQL, Map.of("jobExecutionId", jobExecutionId), Long.class);
        if (alertCount == null || alertCount == 0) {
            return;
        }

        log.warn(
                "決済突合バッチで人手調査が必要なアラートを検知しました。payment_reconciliation_alertsを確認してください。"
                        + " jobExecutionId={}, alertCount={}",
                jobExecutionId,
                alertCount);
        jobExecution.setExitStatus(
                new ExitStatus(
                        EXIT_STATUS_PARTIAL_SUCCESS_WITH_ALERTS,
                        "payment_reconciliation_alertsに" + alertCount + "件の未処理アラートがあります"));
    }
}
