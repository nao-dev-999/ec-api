package com.example.ecapi.batch.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * payment_confirmation_stagingをcustomer_orderへLEFT JOINした1行。
 * customerOrderIdがnullの場合、決済ファイルにはあるがオーダーが存在しない孤立レコードを意味する。
 *
 * <p>orderedAtはcustomer_order.ordered_at。オーソリ成功後にのみ注文が作成される設計（決済失敗時は
 * ロールバックされ注文が残らない）ため、注文作成時刻をオーソリ時刻の近似値としてpayment.authorized_atに使う （customerOrderIdがnullの場合はnull）。
 */
public record PaymentReconciliationRow(
        Long customerOrderId,
        Instant orderedAt,
        String orderNumber,
        String transactionId,
        String status,
        BigDecimal amount,
        BigDecimal fee,
        Instant settledAt) {}
