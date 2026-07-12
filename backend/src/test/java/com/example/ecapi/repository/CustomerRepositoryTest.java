package com.example.ecapi.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.ecapi.config.JpaAuditConfig;
import com.example.ecapi.entity.Customer;
import com.example.ecapi.support.TestcontainersConfiguration;
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
class CustomerRepositoryTest {

    @Autowired private TestEntityManager entityManager;

    @Autowired private CustomerRepository customerRepository;

    private Customer persistCustomer(String email) {
        Customer customer = new Customer();
        customer.setEmail(email);
        customer.setPassword("hashed_password");
        return entityManager.persistFlushFind(customer);
    }

    @Nested
    @DisplayName("findByEmail")
    class FindByEmailTest {

        @Test
        @DisplayName("指定したメールアドレスの顧客を取得できること")
        void shouldReturnCustomerByEmail() {
            persistCustomer("test@example.com");

            Optional<Customer> result = customerRepository.findByEmail("test@example.com");

            assertThat(result).isPresent();
            assertThat(result.get().getEmail()).isEqualTo("test@example.com");
            assertThat(result.get().getCreatedAt()).isNotNull();
            assertThat(result.get().getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("該当する顧客が存在しない場合、空を返すこと")
        void shouldReturnEmptyWhenCustomerNotFound() {
            Optional<Customer> result = customerRepository.findByEmail("notfound@example.com");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("email の一意制約")
    class UniqueEmailConstraintTest {

        @Test
        @DisplayName("同じメールアドレスは重複して登録できないこと")
        void shouldNotAllowDuplicateEmail() {
            persistCustomer("duplicate@example.com");

            Customer duplicate = new Customer();
            duplicate.setEmail("duplicate@example.com");
            duplicate.setPassword("hashed_password");

            assertThatThrownBy(() -> entityManager.persistFlushFind(duplicate))
                    .isInstanceOf(RuntimeException.class);
        }
    }
}
