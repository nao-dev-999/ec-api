package com.example.ecapi.service.employee.dto;

import com.example.ecapi.constant.EmployeeRole;
import java.time.LocalDateTime;

public record EmployeeResult(
        Long id,
        String email,
        EmployeeRole role,
        String lastName,
        String firstName,
        String lastNameKana,
        String firstNameKana,
        String phoneNumber,
        String postalCode,
        String prefecture,
        String city,
        String addressLine1,
        String addressLine2,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        int version) {}
