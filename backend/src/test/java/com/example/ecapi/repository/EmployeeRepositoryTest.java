package com.example.ecapi.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.ecapi.config.JpaAuditConfig;
import com.example.ecapi.constant.EmployeeRole;
import com.example.ecapi.entity.Employee;
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
class EmployeeRepositoryTest {

    @Autowired private TestEntityManager entityManager;

    @Autowired private EmployeeRepository employeeRepository;

    private Employee persistEmployee(String email) {
        Employee employee = new Employee();
        employee.setEmail(email);
        employee.setPassword("hashed_password");
        employee.setRole(EmployeeRole.ADMIN);
        return entityManager.persistFlushFind(employee);
    }

    @Nested
    @DisplayName("findByEmail")
    class FindByEmailTest {

        @Test
        @DisplayName("指定したメールアドレスの従業員を取得できること")
        void shouldReturnEmployeeByEmail() {
            persistEmployee("employee@example.com");

            Optional<Employee> result = employeeRepository.findByEmail("employee@example.com");

            assertThat(result).isPresent();
            assertThat(result.get().getEmail()).isEqualTo("employee@example.com");
            assertThat(result.get().getRole()).isEqualTo(EmployeeRole.ADMIN);
        }

        @Test
        @DisplayName("該当する従業員が存在しない場合、空を返すこと")
        void shouldReturnEmptyWhenEmployeeNotFound() {
            Optional<Employee> result = employeeRepository.findByEmail("notfound@example.com");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("email の一意制約")
    class UniqueEmailConstraintTest {

        @Test
        @DisplayName("同じメールアドレスは重複して登録できないこと")
        void shouldNotAllowDuplicateEmail() {
            persistEmployee("duplicate@example.com");

            Employee duplicate = new Employee();
            duplicate.setEmail("duplicate@example.com");
            duplicate.setPassword("hashed_password");
            duplicate.setRole(EmployeeRole.SALES);

            assertThatThrownBy(() -> entityManager.persistFlushFind(duplicate))
                    .isInstanceOf(RuntimeException.class);
        }
    }
}
