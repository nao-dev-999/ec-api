package com.example.ecapi.controller.customer.shippingaddress.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateShippingAddressRequest(
        @NotBlank(message = "{validation.shippingAddress.recipientName.notBlank}")
                String recipientName,
        @NotBlank(message = "{validation.shippingAddress.postalCode.notBlank}") String postalCode,
        @NotBlank(message = "{validation.shippingAddress.prefecture.notBlank}") String prefecture,
        @NotBlank(message = "{validation.shippingAddress.city.notBlank}") String city,
        @NotBlank(message = "{validation.shippingAddress.addressLine1.notBlank}")
                String addressLine1,
        String addressLine2,
        @NotBlank(message = "{validation.shippingAddress.phoneNumber.notBlank}") String phoneNumber,
        boolean isDefault) {}
