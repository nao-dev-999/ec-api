package com.example.ecapi.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 商品レビューエンティティ
 *
 * <p>customerId/productId は {@link CartItem} と同様に外部キーの値のみを保持する軽量な関連とし、
 * 表示に必要な商品名・顧客名はサービス層で別途解決する。1顧客につき1商品1件まで（customer_id,
 * product_id の複合UNIQUE制約）。独立したライフサイクルを持たず注文明細等と同様に物理削除の対象とするため、
 * {@link SoftDeletable} は実装しない。
 */
@Entity
@Table(name = "review")
@Getter
@Setter
@NoArgsConstructor
public class Review extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(nullable = false)
    private int rating;

    @Column(length = 1000)
    private String comment;
}
