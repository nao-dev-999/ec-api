package com.example.ecapi.controller.admin.auth.dto;

public record LoginResponse(
        String accessToken, String refreshToken, String tokenType, int expiresInMinutes) {}
