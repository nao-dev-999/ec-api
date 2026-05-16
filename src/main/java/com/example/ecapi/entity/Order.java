package com.example.ecapi.entity;

import com.example.ecapi.constant.OrderStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.*;

/** 注文エンティティ */
@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  // 簡易実装のため顧客名を文字列で保持（本格実装では User エンティティとリレーション）
  @Column(name = "customer_name", nullable = false)
  private String customerName;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private OrderStatus status;

  @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
  private BigDecimal totalAmount;

  // 注文明細（1 対多）
  // CascadeType.ALL + orphanRemoval=true で明細の追加・削除を Order 経由で管理
  @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<OrderItem> items = new ArrayList<>();

  @Column(name = "ordered_at", updatable = false)
  private LocalDateTime orderedAt;

  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", updatable = false)
  private LocalDateTime updatedAt;

  @PrePersist
  protected void onCreate() {
    orderedAt = LocalDateTime.now();
    if (status == null) {
      status = OrderStatus.PENDING;
    }
  }
}
