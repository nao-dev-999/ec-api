package com.example.ecapi.service.customer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.ecapi.entity.Customer;
import com.example.ecapi.exception.CustomerInUseException;
import com.example.ecapi.exception.CustomerNotFoundException;
import com.example.ecapi.repository.CustomerRepository;
import com.example.ecapi.service.customer.dto.CustomerResult;
import java.time.Instant;
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
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock private CustomerRepository customerRepository;

    @InjectMocks private CustomerService customerService;

    private Customer customer;

    @BeforeEach
    void setUp() {
        customer = new Customer();
        customer.setId(1L);
        customer.setEmail("test@example.com");
        customer.setPassword("hashed_password");
        ReflectionTestUtils.setField(customer, "createdAt", Instant.now());
        ReflectionTestUtils.setField(customer, "updatedAt", Instant.now());
    }

    @Nested
    @DisplayName("findAll")
    class FindAllTest {

        @Test
        @DisplayName("全顧客を取得できること")
        void shouldReturnAllCustomers() {
            when(customerRepository.findAll()).thenReturn(List.of(customer));

            List<CustomerResult> result = customerService.findAll();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).id()).isEqualTo(1L);
            assertThat(result.get(0).email()).isEqualTo("test@example.com");
        }

        @Test
        @DisplayName("顧客が0件の場合、空のリストを返すこと")
        void shouldReturnEmptyListWhenNoCustomers() {
            when(customerRepository.findAll()).thenReturn(Collections.emptyList());

            List<CustomerResult> result = customerService.findAll();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findById")
    class FindByIdTest {

        @Test
        @DisplayName("指定したIDの顧客を取得できること")
        void shouldReturnCustomerById() {
            when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));

            CustomerResult result = customerService.findById(1L);

            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.email()).isEqualTo("test@example.com");
        }

        @Test
        @DisplayName("指定したIDの顧客が見つからない場合、CustomerNotFoundException をスローすること")
        void shouldThrowExceptionWhenCustomerNotFound() {
            when(customerRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> customerService.findById(99L))
                    .isInstanceOf(CustomerNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("delete")
    class DeleteTest {

        @Test
        @DisplayName("顧客を削除できること")
        void shouldDeleteCustomer() {
            when(customerRepository.existsById(1L)).thenReturn(true);

            customerService.delete(1L);

            verify(customerRepository).deleteById(1L);
        }

        @Test
        @DisplayName("指定したIDの顧客が見つからない場合、CustomerNotFoundException をスローすること")
        void shouldThrowExceptionWhenCustomerNotFound() {
            when(customerRepository.existsById(99L)).thenReturn(false);

            assertThatThrownBy(() -> customerService.delete(99L))
                    .isInstanceOf(CustomerNotFoundException.class);
        }

        @Test
        @DisplayName("注文履歴のある顧客を削除しようとした場合、CustomerInUseException をスローすること")
        void shouldThrowExceptionWhenCustomerHasOrders() {
            when(customerRepository.existsById(1L)).thenReturn(true);
            doThrow(new org.springframework.dao.DataIntegrityViolationException("FK violation"))
                    .when(customerRepository)
                    .flush();

            assertThatThrownBy(() -> customerService.delete(1L))
                    .isInstanceOf(CustomerInUseException.class);
        }
    }
}
