package com.example.ecapi.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.ecapi.config.JpaAuditConfig;
import com.example.ecapi.entity.CartItem;
import com.example.ecapi.entity.Customer;
import com.example.ecapi.entity.Product;
import com.example.ecapi.support.TestcontainersConfiguration;
import java.math.BigDecimal;
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

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, JpaAuditConfig.class})
class CartItemRepositoryTest {

    @Autowired private TestEntityManager entityManager;

    @Autowired private CartItemRepository cartItemRepository;

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

    private CartItem persistCartItem(Long customerId, Long productId, int quantity) {
        CartItem cartItem = new CartItem();
        cartItem.setCustomerId(customerId);
        cartItem.setProductId(productId);
        cartItem.setQuantity(quantity);
        return entityManager.persistFlushFind(cartItem);
    }

    @Nested
    @DisplayName("findAllByCustomerId")
    class FindAllByCustomerIdTest {

        @Test
        @DisplayName("指定した顧客のカート商品を全件取得できること")
        void shouldReturnAllItemsForCustomer() {
            Customer customer = persistCustomer("cart-customer@example.com");
            Product product1 = persistProduct("商品A");
            Product product2 = persistProduct("商品B");
            persistCartItem(customer.getId(), product1.getId(), 2);
            persistCartItem(customer.getId(), product2.getId(), 1);

            List<CartItem> result = cartItemRepository.findAllByCustomerId(customer.getId());

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("カートが空の場合、空のリストを返すこと")
        void shouldReturnEmptyListWhenCartIsEmpty() {
            Customer customer = persistCustomer("empty-cart-customer@example.com");

            List<CartItem> result = cartItemRepository.findAllByCustomerId(customer.getId());

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByCustomerIdAndProductId")
    class FindByCustomerIdAndProductIdTest {

        @Test
        @DisplayName("顧客と商品の組み合わせでカート商品を取得できること")
        void shouldReturnCartItem() {
            Customer customer = persistCustomer("find-customer@example.com");
            Product product = persistProduct("商品C");
            persistCartItem(customer.getId(), product.getId(), 3);

            Optional<CartItem> result =
                    cartItemRepository.findByCustomerIdAndProductId(
                            customer.getId(), product.getId());

            assertThat(result).isPresent();
            assertThat(result.get().getQuantity()).isEqualTo(3);
        }

        @Test
        @DisplayName("該当するカート商品が存在しない場合、空を返すこと")
        void shouldReturnEmptyWhenNotFound() {
            Customer customer = persistCustomer("no-item-customer@example.com");

            Optional<CartItem> result =
                    cartItemRepository.findByCustomerIdAndProductId(customer.getId(), 9999L);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("deleteByCustomerId")
    class DeleteByCustomerIdTest {

        @Test
        @DisplayName("指定した顧客のカート商品を一括削除できること")
        void shouldDeleteAllItemsForCustomer() {
            Customer customer = persistCustomer("delete-customer@example.com");
            Product product1 = persistProduct("商品D");
            Product product2 = persistProduct("商品E");
            persistCartItem(customer.getId(), product1.getId(), 1);
            persistCartItem(customer.getId(), product2.getId(), 1);

            cartItemRepository.deleteByCustomerId(customer.getId());
            entityManager.flush();
            entityManager.clear();

            assertThat(cartItemRepository.findAllByCustomerId(customer.getId())).isEmpty();
        }
    }
}
