package com.example.ecapi.controller.admin.employee.dto;

import com.example.ecapi.constant.EmployeeRole;
import java.time.LocalDateTime;

public record AdminEmployeeResponse(
        Long id,
        String email,
        EmployeeRole role,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        int version) {}
