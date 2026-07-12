package com.example.ecapi.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.ecapi.config.JpaAuditConfig;
import com.example.ecapi.constant.OrderStatus;
import com.example.ecapi.entity.Customer;
import com.example.ecapi.entity.CustomerOrder;
import com.example.ecapi.entity.CustomerOrderDetail;
import com.example.ecapi.entity.Product;
import com.example.ecapi.support.TestcontainersConfiguration;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, JpaAuditConfig.class})
class CustomerOrderRepositoryTest {

    @Autowired private TestEntityManager entityManager;

    @Autowired private CustomerOrderRepository customerOrderRepository;

    private Customer persistCustomer(String email) {
        Customer customer = new Customer();
        customer.setEmail(email);
        customer.setPassword("hashed_password");
        return entityManager.persistFlushFind(customer);
    }

    private Product persistProduct(String name) {
        Product product = new Product();
        product.setName(name);
        product.setPrice(BigDecimal.valueOf(1000));
        product.setStock(10);
        return entityManager.persistFlushFind(product);
    }

    private CustomerOrder persistOrder(Customer customer, OrderStatus status) {
        CustomerOrder order = new CustomerOrder();
        order.setCustomer(customer);
        order.setStatus(status);
        order.setOrderedAt(Instant.now());
        order.setTotalAmount(BigDecimal.valueOf(1000));
        return entityManager.persistFlushFind(order);
    }

    private CustomerOrderDetail persistOrderDetail(CustomerOrder order, Product product) {
        CustomerOrderDetail detail = new CustomerOrderDetail();
        detail.setOrder(order);
        detail.setProduct(product);
        detail.setQuantity(1);
        detail.setUnitPrice(product.getPrice());
        detail.setSubtotal(product.getPrice());
        return entityManager.persistFlushFind(detail);
    }

    @Nested
    @DisplayName("findByStatus")
    class FindByStatusTest {

        @Test
        @DisplayName("指定したステータスの注文のみ取得できること")
        void shouldReturnOrdersWithMatchingStatus() {
            Customer customer = persistCustomer("status-customer@example.com");
            CustomerOrder pending = persistOrder(customer, OrderStatus.PENDING);
            persistOrder(customer, OrderStatus.SHIPPED);

            List<CustomerOrder> result = customerOrderRepository.findByStatus(OrderStatus.PENDING);

            assertThat(result).extracting(CustomerOrder::getId).containsExactly(pending.getId());
        }
    }

    @Nested
    @DisplayName("findByIdWithItems")
    class FindByIdWithItemsTest {

        @Test
        @DisplayName("注文明細と商品を JOIN FETCH した状態で取得できること")
        void shouldReturnOrderWithItemsAndProduct() {
            Customer customer = persistCustomer("items-customer@example.com");
            Product product = persistProduct("注文商品");
            CustomerOrder order = persistOrder(customer, OrderStatus.PENDING);
            persistOrderDetail(order, product);
            entityManager.clear();

            Optional<CustomerOrder> result =
                    customerOrderRepository.findByIdWithItems(order.getId());

            assertThat(result).isPresent();
            assertThat(result.get().getItems()).hasSize(1);
            assertThat(result.get().getItems().get(0).getProduct().getName()).isEqualTo("注文商品");
        }

        @Test
        @DisplayName("存在しない注文 ID の場合、空を返すこと")
        void shouldReturnEmptyWhenOrderNotFound() {
            Optional<CustomerOrder> result = customerOrderRepository.findByIdWithItems(9999L);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findAllByCustomerId")
    class FindAllByCustomerIdTest {

        @Test
        @DisplayName("指定した顧客の注文をページングで取得できること")
        void shouldReturnPagedOrdersForCustomer() {
            Customer customer = persistCustomer("paging-customer@example.com");
            Customer otherCustomer = persistCustomer("other-customer@example.com");
            persistOrder(customer, OrderStatus.PENDING);
            persistOrder(customer, OrderStatus.SHIPPED);
            persistOrder(otherCustomer, OrderStatus.PENDING);
            entityManager.clear();

            Page<CustomerOrder> result =
                    customerOrderRepository.findAllByCustomerId(
                            customer.getId(), PageRequest.of(0, 10));

            assertThat(result.getTotalElements()).isEqualTo(2);
            assertThat(result.getContent())
                    .allSatisfy(
                            order ->
                                    assertThat(order.getCustomer().getId())
                                            .isEqualTo(customer.getId()));
        }
    }

    @Nested
    @DisplayName("findByIdAndCustomerIdWithItems")
    class FindByIdAndCustomerIdWithItemsTest {

        @Test
        @DisplayName("注文 ID と顧客 ID が一致する場合、明細付きで取得できること")
        void shouldReturnOrderWhenIdAndCustomerIdMatch() {
            Customer customer = persistCustomer("match-customer@example.com");
            Product product = persistProduct("一致商品");
            CustomerOrder order = persistOrder(customer, OrderStatus.PENDING);
            persistOrderDetail(order, product);
            entityManager.clear();

            Optional<CustomerOrder> result =
                    customerOrderRepository.findByIdAndCustomerIdWithItems(
                            order.getId(), customer.getId());

            assertThat(result).isPresent();
            assertThat(result.get().getItems()).hasSize(1);
        }

        @Test
        @DisplayName("顧客 ID が一致しない場合、空を返すこと")
        void shouldReturnEmptyWhenCustomerIdDoesNotMatch() {
            Customer customer = persistCustomer("owner-customer@example.com");
            Customer otherCustomer = persistCustomer("stranger-customer@example.com");
            CustomerOrder order = persistOrder(customer, OrderStatus.PENDING);
            entityManager.clear();

            Optional<CustomerOrder> result =
                    customerOrderRepository.findByIdAndCustomerIdWithItems(
                            order.getId(), otherCustomer.getId());

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findAll(Pageable)")
    class FindAllPageableTest {

        @Test
        @DisplayName("顧客を JOIN FETCH した状態で全件をページングで取得できること")
        void shouldReturnAllOrdersWithCustomerJoined() {
            Customer customer = persistCustomer("all-orders-customer@example.com");
            persistOrder(customer, OrderStatus.PENDING);
            persistOrder(customer, OrderStatus.SHIPPED);
            entityManager.clear();

            Page<CustomerOrder> result = customerOrderRepository.findAll(PageRequest.of(0, 10));

            assertThat(result.getTotalElements()).isEqualTo(2);
            assertThat(result.getContent())
                    .allSatisfy(order -> assertThat(order.getCustomer()).isNotNull());
        }
    }
}
