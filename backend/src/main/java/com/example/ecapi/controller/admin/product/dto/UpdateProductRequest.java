package com.example.ecapi.controller.admin.product.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** PUT /api/admin/products/{id} 用の更新リクエスト（部分更新） null == 変更しない（versionは楽観ロックのため必須） */
public record UpdateProductRequest(
        Long id,
        String name,
        String description,
        BigDecimal price,
        Integer stock,
        @Size(max = 2048, message = "{validation.product.imageUrl.size}") String imageUrl,
        @NotNull Integer version) {}
