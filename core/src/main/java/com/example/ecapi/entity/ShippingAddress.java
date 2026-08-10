package com.example.ecapi.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 配送先住所エンティティ（住所帳）
 *
 * <p>customerId は {@link CartItem} と同様に外部キーの値のみを保持する軽量な関連とし、表示に必要な顧客情報はサービス層で別途解決する。
 * 1顧客につき複数件登録可能で、そのうち最大1件を {@code isDefault} とする。
 */
@Entity
@Table(name = "shipping_address")
@Getter
@Setter
@NoArgsConstructor
public class ShippingAddress extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "recipient_name", nullable = false, length = 100)
    private String recipientName;

    @Column(name = "postal_code", nullable = false, length = 10)
    private String postalCode;

    @Column(nullable = false)
    private String prefecture;

    @Column(nullable = false)
    private String city;

    @Column(name = "address_line1", nullable = false)
    private String addressLine1;

    @Column(name = "address_line2")
    private String addressLine2;

    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault = false;
}
