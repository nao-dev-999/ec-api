package com.example.ecapi.controller.admin.category.dto;

import java.time.LocalDateTime;

public record AdminCategoryResponse(
        Long id, String name, LocalDateTime createdAt, LocalDateTime updatedAt, int version) {}
