package com.example.ecapi.controller.customer.shippingaddress.dto;

/** PUT /api/customer/shipping-addresses/{id} 用の更新リクエスト（部分更新） null == 変更しない（versionは楽観ロックのため必須） */
public record UpdateShippingAddressRequest(
        String recipientName,
        String postalCode,
        String prefecture,
        String city,
        String addressLine1,
        String addressLine2,
        String phoneNumber,
        Boolean isDefault,
        int version) {}
