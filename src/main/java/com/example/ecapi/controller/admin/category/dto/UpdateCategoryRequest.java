package com.example.ecapi.controller.admin.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateCategoryRequest(
        @NotBlank(message = "{validation.category.name.notBlank}") String name,
        @NotNull int version) {}
