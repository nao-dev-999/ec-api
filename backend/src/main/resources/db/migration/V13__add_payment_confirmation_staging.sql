-- JobAが受信I/F（決済確定明細CSV）のフォーマット検証後、内容をそのまま取り込む作業用テーブル。
-- job_instance_id（同一対象日に対して再実行を跨いで不変）+ order_id をPKにすることで、
-- 同一Jobインスタンスの同一注文の二重ステージングを防ぐ。
-- 現時点ではJobB以降での参照はなく、監査・将来の突合処理向けに保持するのみ。
-- 作業用テーブルのため、監査カラム・論理削除カラムは持たない。
CREATE TABLE payment_confirmation_staging (
    job_instance_id BIGINT         NOT NULL,
    order_id        BIGINT         NOT NULL,
    amount          DECIMAL(19, 2) NOT NULL,
    settled_at      TIMESTAMPTZ    NOT NULL,
    PRIMARY KEY (job_instance_id, order_id)
);
