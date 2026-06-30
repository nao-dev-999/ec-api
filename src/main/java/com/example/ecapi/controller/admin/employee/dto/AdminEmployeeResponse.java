package com.example.ecapi.controller.admin.employee.dto;

import java.time.LocalDateTime;

public record AdminEmployeeResponse(
        Long id,
        String email,
        String role,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        int version) {}
