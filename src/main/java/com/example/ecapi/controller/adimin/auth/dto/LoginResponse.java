package com.example.ecapi.controller.adimin.auth.dto;

public record LoginResponse(
        String accessToken, String refreshToken, String tokenType, int expiresInMinutes) {}
