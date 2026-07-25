package com.example.ecapi.batch.job.dailysales;

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

    PaymentConfirmationFieldSetMapperTest() {
        tokenizer.setNames("order_id", "amount", "settled_at");
    }

    @Test
    @DisplayName("正常な行をPaymentConfirmationRowへ変換すること")
    void shouldMapValidLineToPaymentConfirmationRow() {
        FieldSet fieldSet = tokenizer.tokenize("1001,12800.00,2024-01-15T03:12:45Z");

        PaymentConfirmationRow row = mapper.mapFieldSet(fieldSet);

        assertThat(row.orderId()).isEqualTo(1001L);
        assertThat(row.amount()).isEqualByComparingTo("12800.00");
        assertThat(row.settledAt()).isEqualTo(Instant.parse("2024-01-15T03:12:45Z"));
    }

    @Test
    @DisplayName("amountが数値として解釈できない場合は例外を投げること")
    void shouldThrowWhenAmountIsNotNumeric() {
        FieldSet fieldSet = tokenizer.tokenize("1001,invalid,2024-01-15T03:12:45Z");

        assertThatThrownBy(() -> mapper.mapFieldSet(fieldSet))
                .isInstanceOf(NumberFormatException.class);
    }

    @Test
    @DisplayName("settled_atがISO-8601形式でない場合は例外を投げること")
    void shouldThrowWhenSettledAtIsNotIsoInstant() {
        FieldSet fieldSet = tokenizer.tokenize("1001,12800.00,2024/01/15 03:12:45");

        assertThatThrownBy(() -> mapper.mapFieldSet(fieldSet))
                .isInstanceOf(DateTimeParseException.class);
    }
}
