package com.example.ecapi.controller.customer.me.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdatePasswordRequest(
        @NotBlank(message = "{validation.customer.currentPassword.notBlank}")
                String currentPassword,
        @NotBlank(message = "{validation.customer.password.notBlank}")
                @Size(min = 8, message = "{validation.customer.password.size}")
                String newPassword,
        @NotNull int version) {}
