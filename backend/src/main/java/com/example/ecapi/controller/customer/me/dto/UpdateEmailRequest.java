package com.example.ecapi.controller.customer.me.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateEmailRequest(
        @NotBlank(message = "{validation.customer.email.notBlank}") @Email String email,
        @NotNull int version) {}
