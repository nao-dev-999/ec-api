package com.example.ecapi.batch.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.ecapi.batch.dto.OrderDetailProjection;
import com.example.ecapi.batch.dto.SalesSummaryRow;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SalesSummaryItemProcessorTest {

    private final LocalDate salesDate = LocalDate.of(2024, 1, 15);
    private final SalesSummaryItemProcessor processor = new SalesSummaryItemProcessor(salesDate);

    @Test
    @DisplayName("正常な明細をSalesSummaryRowへ変換すること")
    void shouldMapValidOrderDetailToSalesSummaryRow() {
        OrderDetailProjection item =
                new OrderDetailProjection(1L, 100L, 200L, new BigDecimal("1000"), 3);

        SalesSummaryRow row = processor.process(item);

        assertThat(row.orderDetailId()).isEqualTo(1L);
        assertThat(row.productId()).isEqualTo(100L);
        assertThat(row.salesDate()).isEqualTo(salesDate);
        assertThat(row.amount()).isEqualByComparingTo("3000");
        assertThat(row.quantity()).isEqualTo(3);
    }

    @Test
    @DisplayName("unitPriceがnullの場合はInvalidOrderDetailExceptionを投げること")
    void shouldThrowWhenUnitPriceIsNull() {
        OrderDetailProjection item = new OrderDetailProjection(1L, 100L, 200L, null, 3);

        assertThatThrownBy(() -> processor.process(item))
                .isInstanceOf(InvalidOrderDetailException.class)
                .hasMessageContaining("1");
    }

    @Test
    @DisplayName("unitPriceが負の場合はInvalidOrderDetailExceptionを投げること")
    void shouldThrowWhenUnitPriceIsNegative() {
        OrderDetailProjection item =
                new OrderDetailProjection(1L, 100L, 200L, new BigDecimal("-1"), 3);

        assertThatThrownBy(() -> processor.process(item))
                .isInstanceOf(InvalidOrderDetailException.class);
    }

    @Test
    @DisplayName("quantityが0以下の場合はInvalidOrderDetailExceptionを投げること")
    void shouldThrowWhenQuantityIsNotPositive() {
        OrderDetailProjection item =
                new OrderDetailProjection(1L, 100L, 200L, new BigDecimal("1000"), 0);

        assertThatThrownBy(() -> processor.process(item))
                .isInstanceOf(InvalidOrderDetailException.class);
    }
}
