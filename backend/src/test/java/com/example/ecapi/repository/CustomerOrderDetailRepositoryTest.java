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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, JpaAuditConfig.class})
class CustomerOrderDetailRepositoryTest {

    @Autowired private TestEntityManager entityManager;

    @Autowired private CustomerOrderDetailRepository customerOrderDetailRepository;

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

    private CustomerOrder persistOrder(Customer customer) {
        CustomerOrder order = new CustomerOrder();
        order.setCustomer(customer);
        order.setStatus(OrderStatus.PENDING);
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
    @DisplayName("findAllByOrderIdIn")
    class FindAllByOrderIdInTest {

        @Test
        @DisplayName("複数の注文 ID に紐づく明細を商品付きで取得できること")
        void shouldReturnDetailsForGivenOrderIds() {
            Customer customer = persistCustomer("detail-customer@example.com");
            Product product1 = persistProduct("明細商品A");
            Product product2 = persistProduct("明細商品B");
            CustomerOrder order1 = persistOrder(customer);
            CustomerOrder order2 = persistOrder(customer);
            persistOrderDetail(order1, product1);
            persistOrderDetail(order2, product2);
            CustomerOrder otherOrder = persistOrder(customer);
            persistOrderDetail(otherOrder, product1);
            entityManager.clear();

            List<CustomerOrderDetail> result =
                    customerOrderDetailRepository.findAllByOrderIdIn(
                            List.of(order1.getId(), order2.getId()));

            assertThat(result).hasSize(2);
            assertThat(result)
                    .extracting(d -> d.getProduct().getName())
                    .containsExactlyInAnyOrder("明細商品A", "明細商品B");
        }

        @Test
        @DisplayName("注文 ID を指定しない場合、空のリストを返すこと")
        void shouldReturnEmptyListWhenNoOrderIdsGiven() {
            List<CustomerOrderDetail> result =
                    customerOrderDetailRepository.findAllByOrderIdIn(List.of());

            assertThat(result).isEmpty();
        }
    }
}
