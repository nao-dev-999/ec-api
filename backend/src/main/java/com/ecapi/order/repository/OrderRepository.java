package com.ecapi.order.repository;

import com.ecapi.order.entity.Order;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * SonarQube / Copilot 比較デモ用の最小スタブリポジトリ。 本番の {@code com.example.ecapi.repository} 配下とは独立しており、Spring
 * のリポジトリ スキャン対象にもならない（デモ・学習目的専用。本番コードから参照しないこと）。
 */
public interface OrderRepository extends JpaRepository<Order, Long> {}
