package com.example.ecapi.controller.admin.employee.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateEmployeeRoleRequest(
        @NotBlank(message = "{validation.employee.role.notBlank}") String role,
        @NotNull int version) {}
