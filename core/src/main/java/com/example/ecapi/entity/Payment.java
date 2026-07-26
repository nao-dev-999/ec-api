package com.example.ecapi.entity;

import com.example.ecapi.constant.PaymentStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.*;

/** 決済エンティティ（決済の正マスタ）。売上集計は本テーブルの明細から再計算可能な二次データとする。 */
@Entity
@Table(name = "payment")
@Getter
@Setter
@NoArgsConstructor
public class Payment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 1オーダー1決済を前提とし、customer_order_idにUNIQUE制約を置く（分割・複数回決済は現状スコープ外）。
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_order_id", nullable = false, unique = true)
    private CustomerOrder customerOrder;

    // 決済代行側の取引ID。payment_confirmation_stagingのtransaction_idと対応する突合キー。
    @Column(name = "transaction_id", nullable = false, length = 64)
    private String transactionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal fee;

    @Column(name = "net_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal netAmount;

    @Column(name = "authorized_at", nullable = false)
    private Instant authorizedAt;

    @Column(name = "captured_at")
    private Instant capturedAt;
}
