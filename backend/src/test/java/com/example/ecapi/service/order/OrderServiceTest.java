package com.example.ecapi.service.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.ecapi.constant.OrderStatus;
import com.example.ecapi.entity.Customer;
import com.example.ecapi.entity.CustomerOrder;
import com.example.ecapi.entity.Product;
import com.example.ecapi.exception.InsufficientStockException;
import com.example.ecapi.exception.OrderNotFoundException;
import com.example.ecapi.exception.ProductNotFoundException;
import com.example.ecapi.repository.CustomerOrderRepository;
import com.example.ecapi.repository.CustomerRepository;
import com.example.ecapi.repository.ProductRepository;
import com.example.ecapi.service.order.dto.CreateOrder;
import com.example.ecapi.service.order.dto.CreateOrderItem;
import com.example.ecapi.service.order.dto.OrderResult;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
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
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private CustomerOrderRepository orderRepository;
    @Mock private ProductRepository productRepository;
    @Mock private CustomerRepository customerRepository;

    @InjectMocks private OrderService orderService;

    private CustomerOrder customerOrder;
    private OrderResult orderResult;
    private Product product;
    private Customer customer;

    @BeforeEach
    void setUp() {
        customer = new Customer();
        customer.setId(1L);
        customer.setEmail("Test Customer");

        product = new Product();
        product.setId(1L);
        product.setName("Test Product");
        product.setPrice(BigDecimal.valueOf(100.00));
        product.setStock(10);
        product.setVersion(1);

        customerOrder = new CustomerOrder();
        customerOrder.setId(1L);
        customerOrder.setCustomerName("Test Customer");
        customerOrder.setStatus(OrderStatus.PENDING);
        customerOrder.setTotalAmount(BigDecimal.valueOf(200.00));
        customerOrder.setOrderedAt(Instant.now());
        ReflectionTestUtils.setField(customerOrder, "createdAt", Instant.now());
        ReflectionTestUtils.setField(customerOrder, "updatedAt", Instant.now());
        customerOrder.setVersion(1);

        orderResult =
                new OrderResult(
                        1L,
                        "Test Customer",
                        OrderStatus.PENDING,
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
            List<OrderResult> result = orderService.findAll();
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().id()).isEqualTo(1L);
            assertThat(result.getFirst().customerName()).isEqualTo("Test Customer");
        }

        @Test
        @DisplayName("注文が0件の場合、空のリストを返すこと")
        void shouldReturnEmptyListWhenNoOrders() {
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
            OrderResult result = orderService.findById(1L);
            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.customerName()).isEqualTo("Test Customer");
        }

        @Test
        @DisplayName("指定したIDの注文が見つからない場合、OrderNotFoundException をスローすること")
        void shouldThrowExceptionWhenOrderNotFound() {
            when(orderRepository.findByIdWithItems(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.findById(99L))
                    .isInstanceOf(OrderNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("findAllByCustomerId")
    class FindAllByCustomerIdTest {

        @Test
        @DisplayName("指定した顧客の注文のみを取得できること")
        void shouldReturnOrdersForCustomer() {
            when(orderRepository.findAllByCustomerId(1L)).thenReturn(List.of(customerOrder));
            List<OrderResult> result = orderService.findAllByCustomerId(1L);
            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("findByIdForCustomer")
    class FindByIdForCustomerTest {

        @Test
        @DisplayName("自分の注文であれば取得できること")
        void shouldReturnOrderWhenOwnedByCustomer() {
            when(orderRepository.findByIdAndCustomerIdWithItems(1L, 1L))
                    .thenReturn(Optional.of(customerOrder));
            OrderResult result = orderService.findByIdForCustomer(1L, 1L);
            assertThat(result.id()).isEqualTo(1L);
        }

        @Test
        @DisplayName("他の顧客の注文の場合、OrderNotFoundException をスローすること")
        void shouldThrowExceptionWhenNotOwnedByCustomer() {
            when(orderRepository.findByIdAndCustomerIdWithItems(1L, 2L))
                    .thenReturn(Optional.empty());
            assertThatThrownBy(() -> orderService.findByIdForCustomer(1L, 2L))
                    .isInstanceOf(OrderNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("create")
    class CreateTest {

        @Test
        @DisplayName("注文を正常に作成できること")
        void shouldCreateOrder() {
            CreateOrder createOrder = new CreateOrder(1L, List.of(new CreateOrderItem(1L, 2)));
            when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
            when(orderRepository.save(any(CustomerOrder.class))).thenReturn(customerOrder);
            OrderResult result = orderService.create(createOrder);
            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.customerName()).isEqualTo("Test Customer");
            verify(orderRepository).save(any(CustomerOrder.class));
        }

        @Test
        @DisplayName("注文対象の商品が存在しない場合、ProductNotFoundException をスローすること")
        void shouldThrowExceptionWhenProductNotFound() {
            CreateOrder createOrder = new CreateOrder(1L, List.of(new CreateOrderItem(99L, 2)));

            when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
            when(productRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.create(createOrder))
                    .isInstanceOf(ProductNotFoundException.class);
        }

        @Test
        @DisplayName("在庫が不足している場合、InsufficientStockException をスローすること")
        void shouldThrowExceptionWhenStockInsufficient() {
            CreateOrder createOrder = new CreateOrder(1L, List.of(new CreateOrderItem(1L, 20)));

            when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
            when(productRepository.findById(1L)).thenReturn(Optional.of(product));

            assertThatThrownBy(() -> orderService.create(createOrder))
                    .isInstanceOf(InsufficientStockException.class);
        }

        @Test
        @DisplayName("注文作成後に在庫が減算されること")
        void shouldDecrementStockAfterOrderCreation() {
            CreateOrder createOrder = new CreateOrder(1L, List.of(new CreateOrderItem(1L, 3)));

            when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
            when(orderRepository.save(any(CustomerOrder.class))).thenReturn(customerOrder);

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

            OrderResult result = orderService.updateStatus(1L, OrderStatus.CONFIRMED, 0);

            assertThat(result).isNotNull();
            verify(orderRepository).save(customerOrder);
        }

        @Test
        @DisplayName("指定したIDの注文が見つからない場合、OrderNotFoundException をスローすること")
        void shouldThrowExceptionWhenOrderNotFound() {
            when(orderRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.updateStatus(99L, OrderStatus.CONFIRMED, 0))
                    .isInstanceOf(OrderNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("cancel")
    class CancelTest {

        @Test
        @DisplayName("注文をキャンセルできること")
        void shouldCancelOrder() {
            when(orderRepository.findByIdWithItems(1L)).thenReturn(Optional.of(customerOrder));
            when(orderRepository.save(any(CustomerOrder.class))).thenReturn(customerOrder);

            OrderResult result = orderService.cancel(1L, 0);

            assertThat(result.status()).isEqualTo(OrderStatus.CANCELLED);
            verify(orderRepository).save(customerOrder);
        }

        @Test
        @DisplayName("指定したIDの注文が見つからない場合、OrderNotFoundException をスローすること")
        void shouldThrowExceptionWhenOrderNotFound() {
            when(orderRepository.findByIdWithItems(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.cancel(99L, 0))
                    .isInstanceOf(OrderNotFoundException.class);
        }
    }
}
