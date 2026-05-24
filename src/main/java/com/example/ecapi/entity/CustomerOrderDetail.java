package com.example.ecapi.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.*;

/** 注文明細エンティティ */
@Entity
@Table(name = "customer_order_detail")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerOrderDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_order_id", nullable = false) // 修正: order_id -> customer_order_id
    private CustomerOrder order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "subtotal", nullable = false, precision = 10, scale = 2) // subtotal もカラムとして追加
    private BigDecimal subtotal;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "created_by", length = 255)
    private String createdBy;

    @Column(name = "updated_by", length = 255)
    private String updatedBy;

    @Column(name = "deleted_by", length = 255)
    private String deletedBy;

    @Version
    @Column(nullable = false)
    private int version;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        // TODO 認証ユーザー情報を取得し設定する
        if (this.createdBy == null) { // createdBy がまだ設定されていない場合のみ
            this.createdBy = "system"; // 仮の値
        }
        this.updatedBy = "system"; // 仮の値
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        // TODO 認証ユーザー情報を取得し設定する
        this.updatedBy = "system"; // 仮の値
    }
}
