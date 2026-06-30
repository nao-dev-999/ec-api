package com.example.ecapi.service.category.dto;

import java.time.LocalDateTime;

public record CategoryResult(
        Long id, String name, LocalDateTime createdAt, LocalDateTime updatedAt, int version) {}
