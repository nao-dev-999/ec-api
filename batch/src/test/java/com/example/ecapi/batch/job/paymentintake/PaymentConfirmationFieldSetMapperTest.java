package com.example.ecapi.batch.job.paymentintake;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.ecapi.batch.dto.PaymentConfirmationRow;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.infrastructure.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.infrastructure.item.file.transform.FieldSet;

class PaymentConfirmationFieldSetMapperTest {

    private final DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();
    private final PaymentConfirmationFieldSetMapper mapper =
            new PaymentConfirmationFieldSetMapper();

    private static final String ORDER_NUMBER = "3fa85f64-5717-4562-b3fc-2c963f66afa6";

    PaymentConfirmationFieldSetMapperTest() {
        tokenizer.setNames(
                "order_number",
                "transaction_id",
                "customer_id",
                "payment_method",
                "status",
                "amount",
                "fee",
                "settled_at");
    }

    @Test
    @DisplayName("正常な行をPaymentConfirmationRowへ変換すること")
    void shouldMapValidLineToPaymentConfirmationRow() {
        FieldSet fieldSet =
                tokenizer.tokenize(
                        ORDER_NUMBER
                                + ",txn_8f3c1a2b9d4e,42,CREDIT_CARD,SETTLED,12800.00,372.00,2024-01-15T03:12:45Z");

        PaymentConfirmationRow row = mapper.mapFieldSet(fieldSet);

        assertThat(row.orderNumber()).isEqualTo(ORDER_NUMBER);
        assertThat(row.transactionId()).isEqualTo("txn_8f3c1a2b9d4e");
        assertThat(row.customerId()).isEqualTo(42L);
        assertThat(row.paymentMethod()).isEqualTo("CREDIT_CARD");
        assertThat(row.status()).isEqualTo("SETTLED");
        assertThat(row.amount()).isEqualByComparingTo("12800.00");
        assertThat(row.fee()).isEqualByComparingTo("372.00");
        assertThat(row.settledAt()).isEqualTo(Instant.parse("2024-01-15T03:12:45Z"));
    }

    @Test
    @DisplayName("amountが数値として解釈できない場合は例外を投げること")
    void shouldThrowWhenAmountIsNotNumeric() {
        FieldSet fieldSet =
                tokenizer.tokenize(
                        ORDER_NUMBER
                                + ",txn_8f3c1a2b9d4e,42,CREDIT_CARD,SETTLED,invalid,372.00,2024-01-15T03:12:45Z");

        assertThatThrownBy(() -> mapper.mapFieldSet(fieldSet))
                .isInstanceOf(NumberFormatException.class);
    }

    @Test
    @DisplayName("settled_atがISO-8601形式でない場合は例外を投げること")
    void shouldThrowWhenSettledAtIsNotIsoInstant() {
        FieldSet fieldSet =
                tokenizer.tokenize(
                        ORDER_NUMBER
                                + ",txn_8f3c1a2b9d4e,42,CREDIT_CARD,SETTLED,12800.00,372.00,2024/01/15 03:12:45");

        assertThatThrownBy(() -> mapper.mapFieldSet(fieldSet))
                .isInstanceOf(DateTimeParseException.class);
    }
}
