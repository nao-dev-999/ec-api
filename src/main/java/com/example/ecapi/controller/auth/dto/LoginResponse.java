package com.example.ecapi.controller.auth.dto;

public record LoginResponse(
        String accessToken, String refreshToken, String tokenType, int expiresInMinutes) {}
