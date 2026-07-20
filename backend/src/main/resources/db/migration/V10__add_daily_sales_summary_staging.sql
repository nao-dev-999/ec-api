-- JobBのWorkerが明細のまま書き込む作業用テーブル。
-- job_instance_id（同一対象日に対して再実行を跨いで不変）+ order_detail_id をPKにすることで、
-- 同一Jobインスタンスの同一明細の二重ステージングを防ぐ。
-- Consolidate Step がGROUP BYして最終テーブルへ置換INSERT後、該当job_instance_id分を物理DELETEする
-- 作業用テーブルのため、監査カラム・論理削除カラムは持たない。
CREATE TABLE daily_sales_summary_staging (
    job_instance_id BIGINT       NOT NULL,
    order_detail_id BIGINT       NOT NULL,
    product_id      BIGINT       NOT NULL REFERENCES product(id),
    sales_date      DATE         NOT NULL,
    amount          DECIMAL(19, 2) NOT NULL,
    quantity        INTEGER      NOT NULL,
    PRIMARY KEY (job_instance_id, order_detail_id)
);
