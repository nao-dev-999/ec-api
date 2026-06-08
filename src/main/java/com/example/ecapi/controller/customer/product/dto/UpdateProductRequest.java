package com.example.ecapi.controller.customer.product.dto;

import java.math.BigDecimal;

/** PATCH /api/products/{id} 用の更新リクエスト（部分更新） すべてのフィールドが省略可能（null == 変更しない） */
public record UpdateProductRequest(
        String name, String description, BigDecimal price, Integer stock, Integer version) {}
