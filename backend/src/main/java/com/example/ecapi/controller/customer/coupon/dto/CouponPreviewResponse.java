package com.example.ecapi.controller.customer.coupon.dto;

import java.math.BigDecimal;

public record CouponPreviewResponse(String code, BigDecimal discountAmount) {}
