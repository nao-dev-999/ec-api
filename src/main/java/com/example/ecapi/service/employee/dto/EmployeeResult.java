package com.example.ecapi.service.employee.dto;

import java.time.LocalDateTime;

public record EmployeeResult(
        Long id,
        String email,
        String role,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        int version) {}
