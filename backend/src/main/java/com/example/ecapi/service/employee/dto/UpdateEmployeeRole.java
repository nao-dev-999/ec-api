package com.example.ecapi.service.employee.dto;

import com.example.ecapi.constant.EmployeeRole;

public record UpdateEmployeeRole(Long id, EmployeeRole role, int version) {}
