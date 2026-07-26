package com.example.ecapi.batch.job.dailysales;

import com.example.ecapi.batch.dto.PaymentReconciliationRow;
import com.example.ecapi.batch.dto.PaymentUpsertRow;
import com.example.ecapi.batch.exception.PaymentReconciliationException;
import com.example.ecapi.constant.OrderPaymentStatus;
import com.example.ecapi.constant.PaymentStatus;
import java.time.Instant;
import org.springframework.batch.infrastructure.item.ItemProcessor;

/**
 * 決済確定ファイルのstatus文字列（決済代行が実際に通知してくる値）を内部の{@link PaymentStatus} （payment）/ {@link
 * OrderPaymentStatus}（customer_order）へマッピングする。 決済代行からの通知は確定後の状態のみのため、AUTHORIZEDはこのファイルには出現しない前提。
 *
 * <p>customer_orderが存在しない（孤立レコード）場合、および未知のstatus値の場合はいずれも 自動処理せず{@link
 * PaymentReconciliationException}をskip対象として投げ、 {@link
 * PaymentReconciliationSkipListener}経由でアラート記録・人手調査に回す。
 */
public class PaymentReconciliationItemProcessor
        implements ItemProcessor<PaymentReconciliationRow, PaymentUpsertRow> {

    @Override
    public PaymentUpsertRow process(PaymentReconciliationRow item) {
        if (item.customerOrderId() == null) {
            throw new PaymentReconciliationException(
                    "決済ファイルにあるがオーダーが存在しません: orderNumber="
                            + item.orderNumber()
                            + ", transactionId="
                            + item.transactionId());
        }

        PaymentStatus status = mapPaymentStatus(item);
        OrderPaymentStatus orderPaymentStatus = mapOrderPaymentStatus(status);

        // captured_atは「実際に決済確定した日時」を表すため、CAPTURED以外のイベント（FAILED/REFUNDED）では
        // settled_atを流用せずnullにする。null時はWriter側のON CONFLICTで既存値を保持する
        // （REFUNDEDが後続で来ても元のCAPTURED時点のcaptured_atを上書きしないため）。
        Instant capturedAt = status == PaymentStatus.CAPTURED ? item.settledAt() : null;

        return new PaymentUpsertRow(
                item.customerOrderId(),
                item.transactionId(),
                status,
                orderPaymentStatus,
                item.amount(),
                item.fee(),
                item.amount().subtract(item.fee()),
                item.orderedAt(),
                capturedAt);
    }

    private PaymentStatus mapPaymentStatus(PaymentReconciliationRow item) {
        return switch (item.status()) {
            case "SETTLED" -> PaymentStatus.CAPTURED;
            case "CANCELLED" -> PaymentStatus.FAILED;
            case "REFUNDED" -> PaymentStatus.REFUNDED;
            default ->
                    throw new PaymentReconciliationException(
                            "決済確定ファイルのstatusが未知の値です: orderNumber="
                                    + item.orderNumber()
                                    + ", transactionId="
                                    + item.transactionId()
                                    + ", status="
                                    + item.status());
        };
    }

    private OrderPaymentStatus mapOrderPaymentStatus(PaymentStatus status) {
        return switch (status) {
            case CAPTURED -> OrderPaymentStatus.CAPTURED;
            case FAILED -> OrderPaymentStatus.CANCELLED;
            case REFUNDED -> OrderPaymentStatus.REFUNDED;
            case AUTHORIZED -> OrderPaymentStatus.AUTHORIZED;
        };
    }
}
