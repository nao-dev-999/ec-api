package com.example.ecapi.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * クーポンエンティティ（定額割引、コード入力式）
 *
 * <p>{@code validFrom}/{@code validTo} はいずれも null 可（未設定側は無期限）。{@code usageLimit} が
 * null の場合は全体の利用回数上限なし。1顧客につき同一クーポンは1回のみ利用可能（サービス層で
 * {@link com.example.ecapi.entity.CustomerOrder#getCouponCode()} の利用履歴から判定）。
 */
@Entity
@Table(name = "coupon")
@Getter
@Setter
@NoArgsConstructor
public class Coupon extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String code;

    @Column(name = "discount_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "valid_from")
    private Instant validFrom;

    @Column(name = "valid_to")
    private Instant validTo;

    @Column(name = "usage_limit")
    private Integer usageLimit;

    @Column(name = "usage_count", nullable = false)
    private int usageCount = 0;

    @Column(nullable = false)
    private boolean active = true;
}
