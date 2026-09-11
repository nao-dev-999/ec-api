package com.ecapi.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ecapi.order.entity.Order;
import com.ecapi.order.entity.OrderItem;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private JdbcTemplate jdbcTemplate;

    @InjectMocks private OrderService orderService;

    @Nested
    @DisplayName("searchOrdersByCustomerName")
    class SearchOrdersByCustomerNameTest {

        @Test
        @DisplayName("プレースホルダを使ったクエリで顧客名をバインドできること")
        void shouldUseParameterizedQuery() {
            when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq("Alice")))
                    .thenReturn(List.of(new Order()));

            List<Order> result = orderService.searchOrdersByCustomerName("Alice");

            assertThat(result).hasSize(1);
            ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
            verify(jdbcTemplate).query(sqlCaptor.capture(), any(RowMapper.class), eq("Alice"));
            assertThat(sqlCaptor.getValue()).contains("?").doesNotContain("Alice");
        }
    }

    @Nested
    @DisplayName("countPendingOrders")
    class CountPendingOrdersTest {

        @Test
        @DisplayName("StatementとResultSetをクローズしつつ件数を返すこと")
        void shouldCloseResourcesAndReturnCount() throws SQLException {
            Connection connection = mock(Connection.class);
            Statement statement = mock(Statement.class);
            ResultSet resultSet = mock(ResultSet.class);
            when(connection.createStatement()).thenReturn(statement);
            when(statement.executeQuery(anyString())).thenReturn(resultSet);
            when(resultSet.getInt(1)).thenReturn(3);

            int count = orderService.countPendingOrders(connection);

            assertThat(count).isEqualTo(3);
            verify(statement).close();
            verify(resultSet).close();
        }
    }

    @Nested
    @DisplayName("classifyOrder")
    class ClassifyOrderTest {

        @Test
        @DisplayName("注文がnullならNULL_ORDERを返すこと")
        void shouldReturnNullOrderWhenOrderIsNull() {
            assertThat(orderService.classifyOrder(null)).isEqualTo("NULL_ORDER");
        }

        @Test
        @DisplayName("金額未設定ならNO_AMOUNTを返すこと")
        void shouldReturnNoAmountWhenTotalAmountIsNull() {
            Order order = new Order();
            assertThat(orderService.classifyOrder(order)).isEqualTo("NO_AMOUNT");
        }

        @Test
        @DisplayName("高額でなければSTANDARDを返すこと")
        void shouldReturnStandardWhenBelowThreshold() {
            Order order = new Order();
            order.setTotalAmount(BigDecimal.valueOf(50_000));
            assertThat(orderService.classifyOrder(order)).isEqualTo("STANDARD");
        }

        @Test
        @DisplayName("高額かつ顧客種別未設定ならUNKNOWN_HIGH_VALUEを返すこと")
        void shouldReturnUnknownHighValueWhenCustomerTypeIsNull() {
            Order order = new Order();
            order.setTotalAmount(BigDecimal.valueOf(200_000));
            assertThat(orderService.classifyOrder(order)).isEqualTo("UNKNOWN_HIGH_VALUE");
        }

        @Test
        @DisplayName("高額かつ非VIPならSTANDARD_HIGH_VALUEを返すこと")
        void shouldReturnStandardHighValueForNonVipCustomer() {
            Order order = new Order();
            order.setTotalAmount(BigDecimal.valueOf(200_000));
            order.setCustomerType("REGULAR");
            assertThat(orderService.classifyOrder(order)).isEqualTo("STANDARD_HIGH_VALUE");
        }

        @Test
        @DisplayName("高額なVIPで商品点数が10以下ならVIP_HIGH_VALUEを返すこと")
        void shouldReturnVipHighValueForSmallVipOrder() {
            Order order = new Order();
            order.setTotalAmount(BigDecimal.valueOf(200_000));
            order.setCustomerType("VIP");
            order.setItems(List.of(new OrderItem()));
            assertThat(orderService.classifyOrder(order)).isEqualTo("VIP_HIGH_VALUE");
        }

        @Test
        @DisplayName("高額なVIPで商品点数が10を超えるならVIP_BULK_HIGH_VALUEを返すこと")
        void shouldReturnVipBulkHighValueForLargeVipOrder() {
            Order order = new Order();
            order.setTotalAmount(BigDecimal.valueOf(200_000));
            order.setCustomerType("VIP");
            order.setItems(
                    List.of(
                            new OrderItem(),
                            new OrderItem(),
                            new OrderItem(),
                            new OrderItem(),
                            new OrderItem(),
                            new OrderItem(),
                            new OrderItem(),
                            new OrderItem(),
                            new OrderItem(),
                            new OrderItem(),
                            new OrderItem()));
            assertThat(orderService.classifyOrder(order)).isEqualTo("VIP_BULK_HIGH_VALUE");
        }
    }

    @Nested
    @DisplayName("calculateFinalPrice")
    class CalculateFinalPriceTest {

        @Test
        @DisplayName("割引を適用してから税金を計算すること")
        void shouldApplyDiscountBeforeTax() {
            BigDecimal result =
                    orderService.calculateFinalPrice(
                            BigDecimal.valueOf(1000),
                            BigDecimal.valueOf(0.1),
                            BigDecimal.valueOf(0.1));

            assertThat(result).isEqualByComparingTo(BigDecimal.valueOf(990));
        }
    }

    @Nested
    @DisplayName("isOrderCancellable")
    class IsOrderCancellableTest {

        @Test
        @DisplayName("PENDINGならキャンセル可能であること")
        void shouldBeCancellableWhenPending() {
            Order order = new Order();
            order.setStatus("PENDING");
            assertThat(orderService.isOrderCancellable(order)).isTrue();
        }

        @Test
        @DisplayName("SHIPPEDならキャンセル不可であること")
        void shouldNotBeCancellableWhenShipped() {
            Order order = new Order();
            order.setStatus("SHIPPED");
            assertThat(orderService.isOrderCancellable(order)).isFalse();
        }

        @Test
        @DisplayName("その他のステータスならキャンセル不可であること")
        void shouldNotBeCancellableForOtherStatuses() {
            Order order = new Order();
            order.setStatus("CANCELLED");
            assertThat(orderService.isOrderCancellable(order)).isFalse();
        }
    }

    @Nested
    @DisplayName("applyItemDiscount / recalculateOrderTotal")
    class DiscountAndTotalTest {

        @Test
        @DisplayName("割引適用後の単価を返すこと")
        void shouldReturnDiscountedUnitPrice() {
            OrderItem item = new OrderItem();
            item.setUnitPrice(BigDecimal.valueOf(100));
            item.setDiscountRate(BigDecimal.valueOf(0.2));

            assertThat(orderService.applyItemDiscount(item))
                    .isEqualByComparingTo(BigDecimal.valueOf(80));
        }

        @Test
        @DisplayName("割引適用後の単価に数量を掛けた合計を返すこと")
        void shouldSumDiscountedItemTotals() {
            OrderItem item1 = new OrderItem();
            item1.setUnitPrice(BigDecimal.valueOf(100));
            item1.setDiscountRate(BigDecimal.valueOf(0.1));
            item1.setQuantity(2);

            OrderItem item2 = new OrderItem();
            item2.setUnitPrice(BigDecimal.valueOf(50));
            item2.setDiscountRate(BigDecimal.ZERO);
            item2.setQuantity(3);

            Order order = new Order();
            order.setItems(List.of(item1, item2));

            assertThat(orderService.recalculateOrderTotal(order))
                    .isEqualByComparingTo(BigDecimal.valueOf(330));
        }
    }
}
