package com.example.ecapi.batch.job;

import com.example.ecapi.batch.dto.OrderDetailProjection;
import com.example.ecapi.batch.dto.SalesSummaryRow;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.batch.infrastructure.item.ItemProcessor;

public class SalesSummaryItemProcessor
        implements ItemProcessor<OrderDetailProjection, SalesSummaryRow> {

    private final LocalDate salesDate;

    public SalesSummaryItemProcessor(LocalDate salesDate) {
        this.salesDate = salesDate;
    }

    @Override
    public SalesSummaryRow process(OrderDetailProjection item) {
        validate(item);
        BigDecimal amount = item.unitPrice().multiply(BigDecimal.valueOf(item.quantity()));
        return new SalesSummaryRow(item.id(), item.productId(), salesDate, amount, item.quantity());
    }

    private void validate(OrderDetailProjection item) {
        if (item.unitPrice() == null || item.unitPrice().signum() < 0) {
            throw new InvalidOrderDetailException(
                    "unitPriceが不正です: orderDetailId="
                            + item.id()
                            + ", unitPrice="
                            + item.unitPrice());
        }
        if (item.quantity() <= 0) {
            throw new InvalidOrderDetailException(
                    "quantityが不正です: orderDetailId=" + item.id() + ", quantity=" + item.quantity());
        }
    }
}
