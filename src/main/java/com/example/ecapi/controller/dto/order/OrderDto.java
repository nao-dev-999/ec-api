package com.example.ecapi.controller.dto.order;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;

public sealed interface OrderDto {

  record OrderItemRequest(
      @NotNull Long productId, @Min(value = 1, message = "数量は1以上必要です") int quantity)
      implements OrderDto {}

  @Builder
  record OrderRequest(
      @NotBlank(message = "顧客名は必須です") String customerName,
      @NotEmpty(message = "注文商品は1つ以上必要です") List<OrderItemRequest> items)
      implements OrderDto {}

  @Builder
  record OrderItemResponse(
      Long productId, String productName, int quantity, BigDecimal unitPrice, BigDecimal subtotal)
      implements OrderDto {}

  @Builder
  record OrderResponse(
      Long id,
      String customerName,
      String status,
      BigDecimal totalAmount,
      List<OrderItemResponse> items,
      LocalDateTime orderedAt,
      LocalDateTime updatedAt)
      implements OrderDto {}
}
