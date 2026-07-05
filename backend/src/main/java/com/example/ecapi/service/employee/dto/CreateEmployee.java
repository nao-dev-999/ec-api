package com.example.ecapi.service.employee.dto;

import com.example.ecapi.constant.EmployeeRole;

public record CreateEmployee(
        String email,
        String password,
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
        String addressLine2) {}
