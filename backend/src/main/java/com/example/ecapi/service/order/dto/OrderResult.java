package com.example.ecapi.service.order.dto;

import com.example.ecapi.constant.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResult(
        Long id,
        Long customerId,
        String customerName,
        OrderStatus status,
        BigDecimal totalAmount,
        String couponCode,
        BigDecimal discountAmount,
        String shippingRecipientName,
        String shippingPostalCode,
        String shippingPrefecture,
        String shippingCity,
        String shippingAddressLine1,
        String shippingAddressLine2,
        String shippingPhoneNumber,
        List<OrderResultItem> items,
        LocalDateTime orderedAt,
        LocalDateTime updatedAt,
        Integer version) {}
