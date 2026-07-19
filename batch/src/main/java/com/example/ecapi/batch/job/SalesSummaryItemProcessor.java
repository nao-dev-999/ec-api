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
        BigDecimal amount = item.unitPrice().multiply(BigDecimal.valueOf(item.quantity()));
        return new SalesSummaryRow(item.productId(), salesDate, amount, item.quantity());
    }
}
