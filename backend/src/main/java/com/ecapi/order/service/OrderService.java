package com.ecapi.order.service;

import com.ecapi.order.entity.Order;
import com.ecapi.order.entity.OrderItem;
import com.ecapi.order.repository.OrderRepository;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * SonarQube / GitHub Copilot 比較デモ用サンプル。
 *
 * <p>本番の {@code com.example.ecapi} 配下とは独立したパッケージであり、Spring
 * のコンポーネントスキャン対象にもならない（デモ・学習目的専用。本番コードにマージしないこと）。
 */
@Service
public class OrderService {

    private static final BigDecimal HIGH_VALUE_THRESHOLD = BigDecimal.valueOf(100_000);
    private static final int BULK_ITEM_COUNT_THRESHOLD = 10;
    private static final String CUSTOMER_TYPE_VIP = "VIP";
    private static final String STATUS_PENDING = "PENDING";

    @Autowired private OrderRepository orderRepository;

    @Autowired private JdbcTemplate jdbcTemplate;

    public List<Order> searchOrdersByCustomerName(String customerName) {
        String sql = "SELECT * FROM orders WHERE customer_name = ?";
        return jdbcTemplate.query(sql, (rs, rowNum) -> mapRowToOrder(rs), customerName);
    }

    public int countPendingOrders(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
                ResultSet resultSet =
                        statement.executeQuery(
                                "SELECT COUNT(*) FROM orders WHERE status = 'PENDING'")) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }

    public String classifyOrder(Order order) {
        if (order == null) {
            return "NULL_ORDER";
        }
        if (order.getTotalAmount() == null) {
            return "NO_AMOUNT";
        }
        if (order.getTotalAmount().compareTo(HIGH_VALUE_THRESHOLD) <= 0) {
            return "STANDARD";
        }
        if (order.getCustomerType() == null) {
            return "UNKNOWN_HIGH_VALUE";
        }
        if (!CUSTOMER_TYPE_VIP.equals(order.getCustomerType())) {
            return "STANDARD_HIGH_VALUE";
        }
        boolean isBulk =
                order.getItems() != null && order.getItems().size() > BULK_ITEM_COUNT_THRESHOLD;
        return isBulk ? "VIP_BULK_HIGH_VALUE" : "VIP_HIGH_VALUE";
    }

    /** 小計に割引を適用してから税金を計算する。 */
    public BigDecimal calculateFinalPrice(
            BigDecimal subtotal, BigDecimal discountRate, BigDecimal taxRate) {
        BigDecimal discountedSubtotal = subtotal.subtract(subtotal.multiply(discountRate));
        return discountedSubtotal.add(discountedSubtotal.multiply(taxRate));
    }

    public boolean isOrderCancellable(Order order) {
        return STATUS_PENDING.equals(order.getStatus());
    }

    /** 割引適用後の単価を返す。 */
    public BigDecimal applyItemDiscount(OrderItem item) {
        BigDecimal discountAmount = item.getUnitPrice().multiply(item.getDiscountRate());
        return item.getUnitPrice().subtract(discountAmount);
    }

    public BigDecimal recalculateOrderTotal(Order order) {
        BigDecimal total = BigDecimal.ZERO;
        for (OrderItem item : order.getItems()) {
            BigDecimal discountedUnitPrice = applyItemDiscount(item);
            BigDecimal itemTotal =
                    discountedUnitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
            total = total.add(itemTotal);
        }
        return total;
    }

    private Order mapRowToOrder(ResultSet rs) {
        // デモ用の簡易マッピング（本サンプルでは実装省略）
        return new Order();
    }
}
