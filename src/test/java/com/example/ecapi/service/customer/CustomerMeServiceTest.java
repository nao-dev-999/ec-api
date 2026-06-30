package com.example.ecapi.service.customer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.ecapi.entity.Customer;
import com.example.ecapi.exception.CustomerEmailDuplicateException;
import com.example.ecapi.exception.CustomerNotFoundException;
import com.example.ecapi.exception.InvalidCurrentPasswordException;
import com.example.ecapi.repository.CustomerRepository;
import com.example.ecapi.service.customer.dto.CustomerResult;
import com.example.ecapi.service.customer.dto.UpdateCustomerEmail;
import com.example.ecapi.service.customer.dto.UpdateCustomerPassword;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CustomerMeServiceTest {

    @Mock private CustomerRepository customerRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks private CustomerMeService customerMeService;

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
    @DisplayName("findMe")
    class FindMeTest {

        @Test
        @DisplayName("自分の顧客情報を取得できること")
        void shouldReturnCustomerInfo() {
            when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));

            CustomerResult result = customerMeService.findMe(1L);

            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.email()).isEqualTo("test@example.com");
        }

        @Test
        @DisplayName("顧客が見つからない場合、CustomerNotFoundException をスローすること")
        void shouldThrowExceptionWhenCustomerNotFound() {
            when(customerRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> customerMeService.findMe(99L))
                    .isInstanceOf(CustomerNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("updateEmail")
    class UpdateEmailTest {

        @Test
        @DisplayName("メールアドレスを変更できること")
        void shouldUpdateEmail() {
            when(customerRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
            when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
            when(customerRepository.save(customer)).thenReturn(customer);

            CustomerResult result =
                    customerMeService.updateEmail(
                            1L, new UpdateCustomerEmail("new@example.com", 0));

            assertThat(result).isNotNull();
            verify(customerRepository).save(customer);
        }

        @Test
        @DisplayName("メールアドレスが他の顧客と重複している場合、CustomerEmailDuplicateException をスローすること")
        void shouldThrowExceptionWhenEmailDuplicate() {
            Customer other = new Customer();
            other.setId(2L);
            other.setEmail("new@example.com");
            when(customerRepository.findByEmail("new@example.com")).thenReturn(Optional.of(other));

            assertThatThrownBy(
                            () ->
                                    customerMeService.updateEmail(
                                            1L, new UpdateCustomerEmail("new@example.com", 0)))
                    .isInstanceOf(CustomerEmailDuplicateException.class);
        }

        @Test
        @DisplayName("同じメールアドレスに更新する場合は成功すること")
        void shouldSucceedWhenUpdatingToSameEmail() {
            when(customerRepository.findByEmail("test@example.com"))
                    .thenReturn(Optional.of(customer));
            when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
            when(customerRepository.save(customer)).thenReturn(customer);

            CustomerResult result =
                    customerMeService.updateEmail(
                            1L, new UpdateCustomerEmail("test@example.com", 0));

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("顧客が見つからない場合、CustomerNotFoundException をスローすること")
        void shouldThrowExceptionWhenCustomerNotFound() {
            when(customerRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
            when(customerRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(
                            () ->
                                    customerMeService.updateEmail(
                                            99L, new UpdateCustomerEmail("new@example.com", 0)))
                    .isInstanceOf(CustomerNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("updatePassword")
    class UpdatePasswordTest {

        @Test
        @DisplayName("パスワードを変更できること")
        void shouldUpdatePassword() {
            when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
            when(passwordEncoder.matches("current123", "hashed_password")).thenReturn(true);
            when(passwordEncoder.encode(anyString())).thenReturn("new_hashed_password");
            when(customerRepository.save(any(Customer.class))).thenReturn(customer);

            customerMeService.updatePassword(
                    1L, new UpdateCustomerPassword("current123", "newPassword123", 0));

            verify(customerRepository).save(customer);
        }

        @Test
        @DisplayName("現在のパスワードが正しくない場合、InvalidCurrentPasswordException をスローすること")
        void shouldThrowExceptionWhenCurrentPasswordInvalid() {
            when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
            when(passwordEncoder.matches("wrongPassword", "hashed_password")).thenReturn(false);

            assertThatThrownBy(
                            () ->
                                    customerMeService.updatePassword(
                                            1L,
                                            new UpdateCustomerPassword(
                                                    "wrongPassword", "newPassword123", 0)))
                    .isInstanceOf(InvalidCurrentPasswordException.class);
        }

        @Test
        @DisplayName("顧客が見つからない場合、CustomerNotFoundException をスローすること")
        void shouldThrowExceptionWhenCustomerNotFound() {
            when(customerRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(
                            () ->
                                    customerMeService.updatePassword(
                                            99L,
                                            new UpdateCustomerPassword(
                                                    "current123", "newPassword123", 0)))
                    .isInstanceOf(CustomerNotFoundException.class);
        }
    }
}
