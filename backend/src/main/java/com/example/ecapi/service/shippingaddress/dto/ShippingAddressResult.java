package com.example.ecapi.service.shippingaddress.dto;

import java.time.LocalDateTime;

public record ShippingAddressResult(
        Long id,
        String recipientName,
        String postalCode,
        String prefecture,
        String city,
        String addressLine1,
        String addressLine2,
        String phoneNumber,
        boolean isDefault,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        int version) {}
