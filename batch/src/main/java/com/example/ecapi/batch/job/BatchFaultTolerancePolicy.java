package com.example.ecapi.batch.job;

import com.example.ecapi.batch.dto.OrderDetailProjection;
import com.example.ecapi.batch.dto.SalesSummaryRow;
import org.springframework.batch.core.step.builder.ChunkOrientedStepBuilder;
import org.springframework.dao.TransientDataAccessException;

/**
 * jobBWorkerStepの異常系（fault tolerance）ポリシーを正常系のStep定義から切り出したもの。
 *
 * <p>データ不正（{@link InvalidOrderDetailException}）はスキップして処理を継続し、DB接続断等の 一時的なシステムエラー（{@link
 * TransientDataAccessException}）はスキップ対象にせずリトライで区別する。
 */
public class BatchFaultTolerancePolicy {

    private static final int SKIP_LIMIT = 100;
    private static final int RETRY_LIMIT = 3;

    public ChunkOrientedStepBuilder<OrderDetailProjection, SalesSummaryRow> apply(
            ChunkOrientedStepBuilder<OrderDetailProjection, SalesSummaryRow> stepBuilder,
            SalesSummarySkipListener skipListener) {
        return stepBuilder
                .faultTolerant()
                .skip(InvalidOrderDetailException.class)
                .skipLimit(SKIP_LIMIT)
                .retry(TransientDataAccessException.class)
                .retryLimit(RETRY_LIMIT)
                .skipListener(skipListener);
    }
}
