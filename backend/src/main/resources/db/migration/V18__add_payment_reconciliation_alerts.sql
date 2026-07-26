-- 決済突合バッチ（paymentReconciliationStep）が自動処理できなかった行の記録用（監査・アラート目的）。
-- 対象は主に「決済ファイルにはあるがCustomerOrderが存在しない（孤立レコード）」「statusが未知の値」の2ケース。
-- batch_skipped_records（daily_sales_summary側のfault tolerance専用）とはキー構造が異なるため
-- （order_detail_idではなくorder_number/transaction_idで突合するため）別テーブルとして持つ。
CREATE TABLE payment_reconciliation_alerts (
    id               BIGSERIAL     PRIMARY KEY,
    job_execution_id BIGINT        NOT NULL,
    order_number     VARCHAR(36)   NOT NULL,
    transaction_id   VARCHAR(64)   NOT NULL,
    error_message    VARCHAR(1000) NOT NULL,
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_payment_reconciliation_alerts_job_execution_id
    ON payment_reconciliation_alerts (job_execution_id);
