package com.ecapi.order.entity;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * SonarQube / Copilot 比較デモ用の最小スタブエンティティ。 本番の {@code com.example.ecapi.entity} 配下とは独立しており、Spring
 * のコンポーネント スキャン対象にもならない（デモ・学習目的専用。本番コードから参照しないこと）。
 */
@Getter
@Setter
public class OrderItem {

    private BigDecimal unitPrice;
    private BigDecimal discountRate;
    private int quantity;
}
