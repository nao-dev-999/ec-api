package com.example.ecapi.batch.job.dailysales;

import com.example.ecapi.batch.dto.PaymentSettlementProjection;
import com.example.ecapi.batch.dto.PaymentSettlementRow;
import org.springframework.batch.infrastructure.item.ItemProcessor;

/**
 * settlement_cycleは決済サービス側の入金サイクル定義が未確定のため、対象日（YYYYMMDD）をそのまま
 * 単純な文字列として設定する（将来、決済サービス側の入金サイクル定義が確定した時点で見直す）。
 */
public class PaymentSettlementItemProcessor
        implements ItemProcessor<PaymentSettlementProjection, PaymentSettlementRow> {

    private final String settlementCycle;

    public PaymentSettlementItemProcessor(String settlementCycle) {
        this.settlementCycle = settlementCycle;
    }

    @Override
    public PaymentSettlementRow process(PaymentSettlementProjection item) {
        return new PaymentSettlementRow(
                item.orderId(),
                item.paymentId(),
                item.capturedAt(),
                item.amount(),
                item.fee(),
                item.netAmount(),
                settlementCycle);
    }
}
