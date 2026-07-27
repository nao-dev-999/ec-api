package com.example.ecapi.batch.exception;

/**
 * paymentReconciliationStepで自動突合できなかった行を表す。
 * 「決済ファイルにはあるがCustomerOrderが存在しない（孤立レコード）」「statusが未知の値」のいずれも、
 * 受信I/Fそのものの異常ではなく個別レコードの業務データ不整合のため、Job全体は止めずskipして {@link
 * com.example.ecapi.batch.job.paymentintake.PaymentReconciliationSkipListener}経由で アラート記録・人手調査に回す。
 */
public class PaymentReconciliationException extends RuntimeException {

    public PaymentReconciliationException(String message) {
        super(message);
    }
}
