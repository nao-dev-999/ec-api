package com.example.ecapi.service.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.ecapi.config.MessageHelper;
import com.example.ecapi.constant.OrderStatus;
import com.example.ecapi.entity.CustomerOrder;
import com.example.ecapi.entity.Product;
import com.example.ecapi.exception.InsufficientStockException;
import com.example.ecapi.exception.OrderNotFoundException;
import com.example.ecapi.exception.ProductNotFoundException;
import com.example.ecapi.repository.CustomerOrderRepository;
import com.example.ecapi.repository.ProductRepository;
import com.example.ecapi.service.order.dto.CreateOrder;
import com.example.ecapi.service.order.dto.CreateOrderItem;
import com.example.ecapi.service.order.dto.OrderResult;
import com.example.ecapi.service.order.mapper.OrderEntityMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private CustomerOrderRepository orderRepository;
    @Mock private ProductRepository productRepository;
    @Mock private OrderEntityMapper orderEntityMapper;
    @Mock private MessageHelper messageHelper;

    @InjectMocks private OrderService orderService;

    private CustomerOrder customerOrder;
    private OrderResult orderResult;
    private Product product;

    @BeforeEach
    void setUp() {
        product =
                Product.builder()
                        .id(1L)
                        .name("Test Product")
                        .price(BigDecimal.valueOf(100.00))
                        .stock(10)
                        .version(1)
                        .build();

        customerOrder =
                CustomerOrder.builder()
                        .id(1L)
                        .customerName("Test Customer")
                        .status(OrderStatus.PENDING)
                        .totalAmount(BigDecimal.valueOf(200.00))
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .version(1)
                        .build();

        orderResult =
                new OrderResult(
                        1L,
                        "Test Customer",
                        "PENDING",
                        BigDecimal.valueOf(200.00),
                        List.of(),
                        LocalDateTime.now(),
                        LocalDateTime.now(),
                        1);
    }

    @Nested
    @DisplayName("findAll")
    class FindAllTest {

        @Test
        @DisplayName("全注文を取得できること")
        void shouldReturnAllOrders() {
            when(orderRepository.findAll()).thenReturn(List.of(customerOrder));
            when(orderEntityMapper.toOrderResultList(any())).thenReturn(List.of(orderResult));

            List<OrderResult> result = orderService.findAll();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).id()).isEqualTo(1L);
            assertThat(result.get(0).customerName()).isEqualTo("Test Customer");
        }

        @Test
        @DisplayName("注文が0件の場合、空のリストを返すこと")
        void shouldReturnEmptyListWhenNoOrders() {
            when(orderRepository.findAll()).thenReturn(Collections.emptyList());
            when(orderEntityMapper.toOrderResultList(any())).thenReturn(Collections.emptyList());

            List<OrderResult> result = orderService.findAll();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findById")
    class FindByIdTest {

        @Test
        @DisplayName("指定したIDの注文を取得できること")
        void shouldReturnOrderById() {
            when(orderRepository.findByIdWithItems(1L)).thenReturn(Optional.of(customerOrder));
            when(orderEntityMapper.toOrderResult(customerOrder)).thenReturn(orderResult);

            OrderResult result = orderService.findById(1L);

            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.customerName()).isEqualTo("Test Customer");
        }

        @Test
        @DisplayName("指定したIDの注文が見つからない場合、OrderNotFoundException をスローすること")
        void shouldThrowExceptionWhenOrderNotFound() {
            when(orderRepository.findByIdWithItems(99L)).thenReturn(Optional.empty());
            when(messageHelper.get(any(), any())).thenReturn("注文が見つかりません: 99");

            assertThatThrownBy(() -> orderService.findById(99L))
                    .isInstanceOf(OrderNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("create")
    class CreateTest {

        @Test
        @DisplayName("注文を正常に作成できること")
        void shouldCreateOrder() {
            CreateOrder createOrder =
                    new CreateOrder("Test Customer", List.of(new CreateOrderItem(1L, 2)));

            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
            when(orderRepository.save(any(CustomerOrder.class))).thenReturn(customerOrder);
            when(orderEntityMapper.toOrderResult(any(CustomerOrder.class))).thenReturn(orderResult);

            OrderResult result = orderService.create(createOrder);

            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.customerName()).isEqualTo("Test Customer");
            verify(orderRepository).save(any(CustomerOrder.class));
        }

        @Test
        @DisplayName("注文対象の商品が存在しない場合、ProductNotFoundException をスローすること")
        void shouldThrowExceptionWhenProductNotFound() {
            CreateOrder createOrder =
                    new CreateOrder("Test Customer", List.of(new CreateOrderItem(99L, 2)));

            when(productRepository.findById(99L)).thenReturn(Optional.empty());
            when(messageHelper.get(any(), any())).thenReturn("商品が見つかりません: 99");

            assertThatThrownBy(() -> orderService.create(createOrder))
                    .isInstanceOf(ProductNotFoundException.class);
        }

        @Test
        @DisplayName("在庫が不足している場合、InsufficientStockException をスローすること")
        void shouldThrowExceptionWhenStockInsufficient() {
            // stock=10 に対して quantity=20 を注文
            CreateOrder createOrder =
                    new CreateOrder("Test Customer", List.of(new CreateOrderItem(1L, 20)));

            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
            when(messageHelper.get(any(), any(), any(), any())).thenReturn("在庫が不足しています");

            assertThatThrownBy(() -> orderService.create(createOrder))
                    .isInstanceOf(InsufficientStockException.class);
        }

        @Test
        @DisplayName("注文作成後に在庫が減算されること")
        void shouldDecrementStockAfterOrderCreation() {
            CreateOrder createOrder =
                    new CreateOrder("Test Customer", List.of(new CreateOrderItem(1L, 3)));

            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
            when(orderRepository.save(any(CustomerOrder.class))).thenReturn(customerOrder);
            when(orderEntityMapper.toOrderResult(any(CustomerOrder.class))).thenReturn(orderResult);

            orderService.create(createOrder);

            // stock 10 - 3 = 7
            assertThat(product.getStock()).isEqualTo(7);
        }
    }

    @Nested
    @DisplayName("updateStatus")
    class UpdateStatusTest {

        @Test
        @DisplayName("注文ステータスを更新できること")
        void shouldUpdateOrderStatus() {
            when(orderRepository.findById(1L)).thenReturn(Optional.of(customerOrder));
            when(orderRepository.save(any(CustomerOrder.class))).thenReturn(customerOrder);
            when(orderEntityMapper.toOrderResult(any(CustomerOrder.class))).thenReturn(orderResult);

            OrderResult result = orderService.updateStatus(1L, OrderStatus.CONFIRMED);

            assertThat(result).isNotNull();
            verify(orderRepository).save(customerOrder);
        }

        @Test
        @DisplayName("指定したIDの注文が見つからない場合、OrderNotFoundException をスローすること")
        void shouldThrowExceptionWhenOrderNotFound() {
            when(orderRepository.findById(99L)).thenReturn(Optional.empty());
            when(messageHelper.get(any(), any())).thenReturn("注文が見つかりません: 99");

            assertThatThrownBy(() -> orderService.updateStatus(99L, OrderStatus.CONFIRMED))
                    .isInstanceOf(OrderNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("cancel")
    class CancelTest {

        @Test
        @DisplayName("注文をキャンセルできること")
        void shouldCancelOrder() {
            OrderResult cancelledResult =
                    new OrderResult(
                            1L,
                            "Test Customer",
                            "CANCELLED",
                            BigDecimal.valueOf(200.00),
                            List.of(),
                            LocalDateTime.now(),
                            LocalDateTime.now(),
                            1);

            when(orderRepository.findById(1L)).thenReturn(Optional.of(customerOrder));
            when(orderRepository.save(any(CustomerOrder.class))).thenReturn(customerOrder);
            when(orderEntityMapper.toOrderResult(any(CustomerOrder.class)))
                    .thenReturn(cancelledResult);

            OrderResult result = orderService.cancel(1L);

            assertThat(result.status()).isEqualTo("CANCELLED");
            verify(orderRepository).save(customerOrder);
        }

        @Test
        @DisplayName("指定したIDの注文が見つからない場合、OrderNotFoundException をスローすること")
        void shouldThrowExceptionWhenOrderNotFound() {
            when(orderRepository.findById(99L)).thenReturn(Optional.empty());
            when(messageHelper.get(any(), any())).thenReturn("注文が見つかりません: 99");

            assertThatThrownBy(() -> orderService.cancel(99L))
                    .isInstanceOf(OrderNotFoundException.class);
        }
    }
}
