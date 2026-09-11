package com.ecapi.order.entity;

import java.math.BigDecimal;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * SonarQube / Copilot 比較デモ用の最小スタブエンティティ。 本番の {@code com.example.ecapi.entity} 配下とは独立しており、Spring
 * のコンポーネント スキャン対象にもならない（デモ・学習目的専用。本番コードから参照しないこと）。
 */
@Getter
@Setter
public class Order {

    private Long id;
    private BigDecimal totalAmount;
    private String customerType;
    private String status;
    private List<OrderItem> items;
}
