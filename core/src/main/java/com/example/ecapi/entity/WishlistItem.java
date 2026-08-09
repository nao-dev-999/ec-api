package com.example.ecapi.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * お気に入り（ウィッシュリスト）エンティティ
 *
 * <p>customerId/productId は {@link CartItem} と同様に外部キーの値のみを保持する軽量な関連とし、
 * 表示に必要な商品情報はサービス層で別途解決する。1顧客につき同一商品は1件まで （customer_id, product_id の複合UNIQUE制約）。
 */
@Entity
@Table(name = "wishlist_item")
@Getter
@Setter
@NoArgsConstructor
public class WishlistItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "product_id", nullable = false)
    private Long productId;
}
