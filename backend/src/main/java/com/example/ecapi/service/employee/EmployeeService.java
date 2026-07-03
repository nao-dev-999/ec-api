package com.example.ecapi.service.employee;

import com.example.ecapi.entity.Employee;
import com.example.ecapi.exception.EmployeeEmailDuplicateException;
import com.example.ecapi.exception.EmployeeNotFoundException;
import com.example.ecapi.repository.EmployeeRepository;
import com.example.ecapi.service.employee.dto.CreateEmployee;
import com.example.ecapi.service.employee.dto.EmployeeResult;
import com.example.ecapi.service.employee.dto.UpdateEmployeeRole;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;

    public List<EmployeeResult> findAll() {
        return employeeRepository.findAll().stream().map(this::toEmployeeResult).toList();
    }

    public EmployeeResult findById(Long id) {
        return employeeRepository
                .findById(id)
                .map(this::toEmployeeResult)
                .orElseThrow(() -> new EmployeeNotFoundException(id));
    }

    @Transactional
    public EmployeeResult create(CreateEmployee createEmployee) {
        if (employeeRepository.findByEmail(createEmployee.email()).isPresent()) {
            throw new EmployeeEmailDuplicateException(createEmployee.email());
        }
        Employee employee = new Employee();
        employee.setEmail(createEmployee.email());
        employee.setPassword(passwordEncoder.encode(createEmployee.password()));
        employee.setRole(createEmployee.role());
        Employee saved = employeeRepository.save(employee);
        log.info("Employee created employeeId={} role={}", saved.getId(), saved.getRole());
        return toEmployeeResult(saved);
    }

    @Transactional
    public EmployeeResult updateRole(UpdateEmployeeRole updateEmployeeRole) {
        Employee employee =
                employeeRepository
                        .findById(updateEmployeeRole.id())
                        .orElseThrow(() -> new EmployeeNotFoundException(updateEmployeeRole.id()));
        employee.setRole(updateEmployeeRole.role());
        employee.setVersion(updateEmployeeRole.version());
        log.info(
                "Employee role updated employeeId={} role={}",
                updateEmployeeRole.id(),
                updateEmployeeRole.role());
        return toEmployeeResult(employeeRepository.save(employee));
    }

    @Transactional
    public void delete(Long id) {
        if (!employeeRepository.existsById(id)) {
            throw new EmployeeNotFoundException(id);
        }
        log.info("Employee deleted employeeId={}", id);
        employeeRepository.deleteById(id);
    }

    private EmployeeResult toEmployeeResult(Employee employee) {
        return new EmployeeResult(
                employee.getId(),
                employee.getEmail(),
                employee.getRole(),
                LocalDateTime.ofInstant(employee.getCreatedAt(), ZoneId.systemDefault()),
                LocalDateTime.ofInstant(employee.getUpdatedAt(), ZoneId.systemDefault()),
                employee.getVersion());
    }
}
