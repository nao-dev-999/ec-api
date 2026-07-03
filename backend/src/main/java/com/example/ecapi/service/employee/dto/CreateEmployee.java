package com.example.ecapi.service.employee.dto;

import com.example.ecapi.constant.EmployeeRole;

public record CreateEmployee(String email, String password, EmployeeRole role) {}
