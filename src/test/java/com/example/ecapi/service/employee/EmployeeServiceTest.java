package com.example.ecapi.service.employee;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.ecapi.entity.Employee;
import com.example.ecapi.exception.EmployeeEmailDuplicateException;
import com.example.ecapi.exception.EmployeeNotFoundException;
import com.example.ecapi.repository.EmployeeRepository;
import com.example.ecapi.service.employee.dto.CreateEmployee;
import com.example.ecapi.service.employee.dto.EmployeeResult;
import com.example.ecapi.service.employee.dto.UpdateEmployeeRole;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock private EmployeeRepository employeeRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks private EmployeeService employeeService;

    private Employee employee;

    @BeforeEach
    void setUp() {
        employee = new Employee();
        employee.setId(1L);
        employee.setEmail("admin@example.com");
        employee.setPassword("hashed_password");
        employee.setRole("ADMIN");
        ReflectionTestUtils.setField(employee, "createdAt", Instant.now());
        ReflectionTestUtils.setField(employee, "updatedAt", Instant.now());
    }

    @Nested
    @DisplayName("findAll")
    class FindAllTest {

        @Test
        @DisplayName("全従業員を取得できること")
        void shouldReturnAllEmployees() {
            when(employeeRepository.findAll()).thenReturn(List.of(employee));

            List<EmployeeResult> result = employeeService.findAll();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).id()).isEqualTo(1L);
            assertThat(result.get(0).email()).isEqualTo("admin@example.com");
            assertThat(result.get(0).role()).isEqualTo("ADMIN");
        }

        @Test
        @DisplayName("従業員が0件の場合、空のリストを返すこと")
        void shouldReturnEmptyListWhenNoEmployees() {
            when(employeeRepository.findAll()).thenReturn(Collections.emptyList());

            List<EmployeeResult> result = employeeService.findAll();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findById")
    class FindByIdTest {

        @Test
        @DisplayName("指定したIDの従業員を取得できること")
        void shouldReturnEmployeeById() {
            when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));

            EmployeeResult result = employeeService.findById(1L);

            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.email()).isEqualTo("admin@example.com");
        }

        @Test
        @DisplayName("指定したIDの従業員が見つからない場合、EmployeeNotFoundException をスローすること")
        void shouldThrowExceptionWhenEmployeeNotFound() {
            when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> employeeService.findById(99L))
                    .isInstanceOf(EmployeeNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("create")
    class CreateTest {

        @Test
        @DisplayName("従業員を新規登録できること")
        void shouldCreateEmployee() {
            when(employeeRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
            when(passwordEncoder.encode(anyString())).thenReturn("hashed_password");
            when(employeeRepository.save(any(Employee.class))).thenReturn(employee);

            CreateEmployee createEmployee =
                    new CreateEmployee("new@example.com", "password123", "STAFF");
            EmployeeResult result = employeeService.create(createEmployee);

            assertThat(result.id()).isEqualTo(1L);
            verify(employeeRepository).save(any(Employee.class));
        }

        @Test
        @DisplayName("メールアドレスが重複している場合、EmployeeEmailDuplicateException をスローすること")
        void shouldThrowExceptionWhenEmailDuplicate() {
            when(employeeRepository.findByEmail("admin@example.com"))
                    .thenReturn(Optional.of(employee));

            CreateEmployee createEmployee =
                    new CreateEmployee("admin@example.com", "password123", "ADMIN");

            assertThatThrownBy(() -> employeeService.create(createEmployee))
                    .isInstanceOf(EmployeeEmailDuplicateException.class);
        }
    }

    @Nested
    @DisplayName("updateRole")
    class UpdateRoleTest {

        @Test
        @DisplayName("従業員のロールを更新できること")
        void shouldUpdateEmployeeRole() {
            when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
            when(employeeRepository.save(employee)).thenReturn(employee);

            EmployeeResult result =
                    employeeService.updateRole(new UpdateEmployeeRole(1L, "STAFF", 0));

            assertThat(result).isNotNull();
            verify(employeeRepository).save(employee);
        }

        @Test
        @DisplayName("指定したIDの従業員が見つからない場合、EmployeeNotFoundException をスローすること")
        void shouldThrowExceptionWhenEmployeeNotFound() {
            when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(
                            () ->
                                    employeeService.updateRole(
                                            new UpdateEmployeeRole(99L, "STAFF", 0)))
                    .isInstanceOf(EmployeeNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("delete")
    class DeleteTest {

        @Test
        @DisplayName("従業員を削除できること")
        void shouldDeleteEmployee() {
            when(employeeRepository.existsById(1L)).thenReturn(true);

            employeeService.delete(1L);

            verify(employeeRepository).deleteById(1L);
        }

        @Test
        @DisplayName("指定したIDの従業員が見つからない場合、EmployeeNotFoundException をスローすること")
        void shouldThrowExceptionWhenEmployeeNotFound() {
            when(employeeRepository.existsById(99L)).thenReturn(false);

            assertThatThrownBy(() -> employeeService.delete(99L))
                    .isInstanceOf(EmployeeNotFoundException.class);
        }
    }
}
