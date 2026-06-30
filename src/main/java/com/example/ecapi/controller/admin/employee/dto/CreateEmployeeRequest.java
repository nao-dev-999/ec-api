package com.example.ecapi.controller.admin.employee.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateEmployeeRequest(
        @NotBlank(message = "{validation.employee.email.notBlank}") @Email String email,
        @NotBlank(message = "{validation.employee.password.notBlank}")
                @Size(min = 8, message = "{validation.employee.password.size}")
                String password,
        @NotBlank(message = "{validation.employee.role.notBlank}") String role) {}
