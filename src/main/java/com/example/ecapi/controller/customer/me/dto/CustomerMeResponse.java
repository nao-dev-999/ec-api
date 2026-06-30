package com.example.ecapi.controller.customer.me.dto;

import java.time.LocalDateTime;

public record CustomerMeResponse(
        Long id, String email, LocalDateTime createdAt, LocalDateTime updatedAt, int version) {}
