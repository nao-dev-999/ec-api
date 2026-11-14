package com.example.ecapi.service.order;

import com.example.ecapi.service.order.dto.OrderResult;
import com.example.ecapi.service.order.dto.OrderResultItem;
import java.util.List;

/** 注文履歴を CSV 形式の文字列に変換する。明細1件を1行として出力する。 */
final class OrderCsvWriter {

    private static final String HEADER = "注文ID,注文日時,商品名,数量,単価,小計";

    private OrderCsvWriter() {}

    static String write(List<OrderResult> orders) {
        StringBuilder sb = new StringBuilder(HEADER).append("\n");
        for (OrderResult order : orders) {
            for (OrderResultItem item : order.items()) {
                sb.append(order.id())
                        .append(',')
                        .append(order.orderedAt())
                        .append(',')
                        .append(item.productName())
                        .append(',')
                        .append(item.quantity())
                        .append(',')
                        .append(item.unitPrice())
                        .append(',')
                        .append(item.subtotal())
                        .append("\n");
            }
        }
        return sb.toString();
    }
}
