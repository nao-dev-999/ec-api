package com.example.ecapi.controller.admin.category.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateCategoryRequest(
        @NotBlank(message = "{validation.category.name.notBlank}") String name) {}
