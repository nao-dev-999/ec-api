package com.example.ecapi.batch.job.dailysales;

import com.example.ecapi.batch.dto.PaymentReconciliationRow;
import com.example.ecapi.batch.dto.PaymentUpsertRow;
import java.util.Map;
import org.springframework.batch.core.listener.SkipListener;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/**
 * 突合できなかった行（孤立レコード・未知のstatus）を{@code payment_reconciliation_alerts}に記録する。
 * 「決済ファイルにあるがオーダーが存在しない」場合等を自動処理せず人手調査に回すための記録であり、 {@link SalesSummarySkipListener}と同じ設計（fault
 * toleranceでskipされたレコードの監査記録）を踏襲する。 実際の通知先（Slack/メール等）への連携は未決定のため、ここでは記録のみ行う。
 */
public class PaymentReconciliationSkipListener
        implements SkipListener<PaymentReconciliationRow, PaymentUpsertRow> {

    private static final String INSERT_SQL =
            """
            INSERT INTO payment_reconciliation_alerts
                (job_execution_id, order_number, transaction_id, error_message)
            VALUES (:jobExecutionId, :orderNumber, :transactionId, :errorMessage)
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final Long jobExecutionId;

    public PaymentReconciliationSkipListener(
            NamedParameterJdbcTemplate jdbcTemplate, Long jobExecutionId) {
        this.jdbcTemplate = jdbcTemplate;
        this.jobExecutionId = jobExecutionId;
    }

    @Override
    public void onSkipInProcess(PaymentReconciliationRow item, Throwable t) {
        Map<String, Object> params =
                Map.of(
                        "jobExecutionId",
                        jobExecutionId,
                        "orderNumber",
                        item.orderNumber(),
                        "transactionId",
                        item.transactionId(),
                        "errorMessage",
                        String.valueOf(t.getMessage()));
        jdbcTemplate.update(INSERT_SQL, params);
    }
}
