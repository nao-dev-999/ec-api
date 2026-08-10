package com.example.ecapi.service.shippingaddress.dto;

/** Service 層で扱う Update 用 DTO（部分更新） null 値は「変更しない」を意味します（isDefault は必須指定）。 */
public record UpdateShippingAddress(
        Long id,
        Long customerId,
        String recipientName,
        String postalCode,
        String prefecture,
        String city,
        String addressLine1,
        String addressLine2,
        String phoneNumber,
        Boolean isDefault,
        int version) {}
