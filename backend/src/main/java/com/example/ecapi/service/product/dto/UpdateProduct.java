package com.example.ecapi.service.product.dto;

import java.math.BigDecimal;

/** Service 層で扱う Update 用 DTO（部分更新） null 値は「変更しない」を意味します。 */
public record UpdateProduct(
        Long id,
        String name,
        String description,
        BigDecimal price,
        Integer stock,
        Integer version) {}
