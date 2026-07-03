package com.example.ecapi.controller.admin.employee;

import com.example.ecapi.controller.admin.employee.dto.AdminEmployeeResponse;
import com.example.ecapi.controller.admin.employee.dto.CreateEmployeeRequest;
import com.example.ecapi.controller.admin.employee.dto.UpdateEmployeeRoleRequest;
import com.example.ecapi.service.employee.EmployeeService;
import com.example.ecapi.service.employee.dto.CreateEmployee;
import com.example.ecapi.service.employee.dto.EmployeeResult;
import com.example.ecapi.service.employee.dto.UpdateEmployeeRole;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/employees")
@RequiredArgsConstructor
public class AdminEmployeeController {

    private final EmployeeService employeeService;

    @GetMapping
    public ResponseEntity<List<AdminEmployeeResponse>> getAll() {
        return ResponseEntity.ok(employeeService.findAll().stream().map(this::toResponse).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminEmployeeResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(toResponse(employeeService.findById(id)));
    }

    @PostMapping
    public ResponseEntity<AdminEmployeeResponse> create(
            @Valid @RequestBody CreateEmployeeRequest request) {
        EmployeeResult result =
                employeeService.create(
                        new CreateEmployee(request.email(), request.password(), request.role()));
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(result));
    }

    @PatchMapping("/{id}/role")
    public ResponseEntity<AdminEmployeeResponse> updateRole(
            @PathVariable Long id, @Valid @RequestBody UpdateEmployeeRoleRequest request) {
        return ResponseEntity.ok(
                toResponse(
                        employeeService.updateRole(
                                new UpdateEmployeeRole(id, request.role(), request.version()))));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        employeeService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private AdminEmployeeResponse toResponse(EmployeeResult result) {
        return new AdminEmployeeResponse(
                result.id(),
                result.email(),
                result.role(),
                result.createdAt(),
                result.updatedAt(),
                result.version());
    }
}
