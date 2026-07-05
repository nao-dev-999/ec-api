package com.example.ecapi.controller.admin.employee.dto;

import com.example.ecapi.constant.EmployeeRole;
import jakarta.validation.constraints.NotNull;

public record UpdateEmployeeRoleRequest(
        @NotNull(message = "{validation.employee.role.notBlank}") EmployeeRole role,
        @NotNull int version) {}
