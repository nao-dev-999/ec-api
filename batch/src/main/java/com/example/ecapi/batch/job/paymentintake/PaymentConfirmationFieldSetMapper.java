package com.example.ecapi.batch.job.paymentintake;

import com.example.ecapi.batch.dto.PaymentConfirmationRow;
import com.example.ecapi.batch.exception.InvalidOrderDetailException;
import java.time.Instant;
import org.springframework.batch.infrastructure.item.file.mapping.FieldSetMapper;
import org.springframework.batch.infrastructure.item.file.transform.FieldSet;

/**
 * 決済確定明細CSVの1行を{@link PaymentConfirmationRow}へ変換する。
 *
 * <p>数値・日時として解釈できない値がある場合は{@link NumberFormatException}や{@link
 * java.time.format.DateTimeParseException}がそのまま伝播しStepを異常終了させる。集計フェーズのデータ不正（{@link
 * InvalidOrderDetailException}）と異なりskip対象にしない。受信I/Fそのもののフォーマット不正であり、 一部の行だけ無視して集計を続けるのは危険なため。
 */
public class PaymentConfirmationFieldSetMapper implements FieldSetMapper<PaymentConfirmationRow> {

    @Override
    public PaymentConfirmationRow mapFieldSet(FieldSet fieldSet) {
        return new PaymentConfirmationRow(
                fieldSet.readString("order_number"),
                fieldSet.readString("transaction_id"),
                fieldSet.readLong("customer_id"),
                fieldSet.readString("payment_method"),
                fieldSet.readString("status"),
                fieldSet.readBigDecimal("amount"),
                fieldSet.readBigDecimal("fee"),
                Instant.parse(fieldSet.readString("settled_at")));
    }
}
