package com.example.ecapi.controller.dto.product;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;

public sealed interface ProductDto {

  @Builder
  record ProductRequest(
      @NotBlank(message = "商品名は必須です") String name,
      String description,
      @NotNull(message = "価格は必須です") @DecimalMin(value = "0.0", inclusive = false) BigDecimal price,
      @Min(value = 0, message = "在庫数は0以上で指定してください") int stock)
      implements ProductDto {}

  @Builder
  record ProductResponse(
      Long id,
      String name,
      String description,
      BigDecimal price,
      int stock,
      LocalDateTime createdAt,
      LocalDateTime updatedAt)
      implements ProductDto {}
}
