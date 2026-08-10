package com.example.ecapi.service.shippingaddress.dto;

public record CreateShippingAddress(
        Long customerId,
        String recipientName,
        String postalCode,
        String prefecture,
        String city,
        String addressLine1,
        String addressLine2,
        String phoneNumber,
        boolean isDefault) {}
