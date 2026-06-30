package com.example.ecapi.service.customer.dto;

import java.time.LocalDateTime;

public record CustomerResult(
        Long id, String email, LocalDateTime createdAt, LocalDateTime updatedAt, int version) {}
