package com.example.ecapi.entity;

import com.example.ecapi.constant.OrderPaymentStatus;
import com.example.ecapi.constant.OrderStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.*;

/** 注文エンティティ */
@Entity
@Table(name = "customer_order")
@Getter
@Setter
@NoArgsConstructor
public class CustomerOrder extends BaseEntity implements SoftDeletable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 外部システム（決済代行等）との連携で使う参照番号。idはサロゲートキーであり外部に開示しないため別に持つ。
    @Column(name = "order_number", updatable = false, nullable = false, unique = true, length = 36)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    // 発送等の履行ステータス（status）とは別軸。オーソリ成功時にAUTHORIZEDで作成され、
    // 夜間バッチの決済確定突合でCAPTURED/CANCELLED/REFUNDEDへ遷移する。
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 20)
    private OrderPaymentStatus paymentStatus;

    @Column(name = "ordered_at", updatable = false, nullable = false)
    private Instant orderedAt;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    // 適用したクーポンのコード（未使用時はnull）。Couponへの外部キーではなく、
    // クーポンが後から削除・変更されても注文履歴上の適用結果は変わらないようにするため値のみ保持する。
    @Column(name = "coupon_code", length = 30)
    private String couponCode;

    @Column(name = "discount_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    // 配送先住所のスナップショット。ShippingAddressへの外部キーではなく、
    // 住所帳が後から編集・削除されても注文時点の配送先が変わらないよう値のみ保持する。
    @Column(name = "shipping_recipient_name", length = 100)
    private String shippingRecipientName;

    @Column(name = "shipping_postal_code", length = 10)
    private String shippingPostalCode;

    @Column(name = "shipping_prefecture")
    private String shippingPrefecture;

    @Column(name = "shipping_city")
    private String shippingCity;

    @Column(name = "shipping_address_line1")
    private String shippingAddressLine1;

    @Column(name = "shipping_address_line2")
    private String shippingAddressLine2;

    @Column(name = "shipping_phone_number", length = 20)
    private String shippingPhoneNumber;

    // 注文明細（1 対多）。外部からの直接操作を防ぐため、getter/setterはaddItem/removeItem経由に限定する。
    @Setter(AccessLevel.NONE)
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CustomerOrderDetail> items = new ArrayList<>();

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted = false;

    @PrePersist
    private void assignOrderNumberIfAbsent() {
        if (orderNumber == null) {
            orderNumber = UUID.randomUUID().toString();
        }
    }

    public List<CustomerOrderDetail> getItems() {
        return Collections.unmodifiableList(items);
    }

    public void addItem(CustomerOrderDetail detail) {
        if (detail == null) {
            throw new IllegalArgumentException("detail must not be null");
        }
        items.add(detail);
        detail.setOrder(this);
    }

    public void removeItem(CustomerOrderDetail detail) {
        if (detail == null) {
            throw new IllegalArgumentException("detail must not be null");
        }
        if (items.remove(detail)) {
            detail.setOrder(null);
        }
    }
}
