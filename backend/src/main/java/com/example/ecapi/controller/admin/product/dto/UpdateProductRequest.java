package com.example.ecapi.controller.admin.product.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/** PUT /api/admin/products/{id} 用の更新リクエスト（部分更新） null == 変更しない（versionは楽観ロックのため必須） */
public record UpdateProductRequest(
        Long id,
        String name,
        String description,
        BigDecimal price,
        Integer stock,
        @NotNull Integer version) {}
