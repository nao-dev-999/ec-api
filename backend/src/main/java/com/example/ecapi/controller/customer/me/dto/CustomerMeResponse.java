package com.example.ecapi.controller.customer.me.dto;

import java.time.LocalDateTime;

public record CustomerMeResponse(
        Long id,
        String email,
        String lastName,
        String firstName,
        String lastNameKana,
        String firstNameKana,
        String phoneNumber,
        String postalCode,
        String prefecture,
        String city,
        String addressLine1,
        String addressLine2,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        int version) {}
