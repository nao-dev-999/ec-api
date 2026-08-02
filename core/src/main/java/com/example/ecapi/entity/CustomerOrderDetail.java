package com.example.ecapi.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.*;

/** 注文明細エンティティ */
@Entity
@Table(name = "customer_order_detail")
@Getter
@Setter
@NoArgsConstructor
public class CustomerOrderDetail extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // setOrderはpackage-privateに制限し、CustomerOrder.addItem/removeItem経由でのみ同期させる
    @Setter(AccessLevel.NONE)
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

    void setOrder(CustomerOrder order) {
        this.order = order;
    }
}
