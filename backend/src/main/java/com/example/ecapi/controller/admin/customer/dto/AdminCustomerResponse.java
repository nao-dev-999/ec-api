package com.example.ecapi.controller.admin.customer.dto;

import java.time.LocalDateTime;

public record AdminCustomerResponse(
        Long id, String email, LocalDateTime createdAt, LocalDateTime updatedAt, int version) {}
