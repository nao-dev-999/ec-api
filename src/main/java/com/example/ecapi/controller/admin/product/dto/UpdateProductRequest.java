package com.example.ecapi.controller.admin.product.dto;

import java.math.BigDecimal;

/** PUT /api/admin/products/{id} 用の更新リクエスト（部分更新） null == 変更しない */
public record UpdateProductRequest(
        Long id,
        String name,
        String description,
        BigDecimal price,
        Integer stock,
        Integer version) {}
