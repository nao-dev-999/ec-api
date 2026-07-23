package com.example.ecapi.repository.support;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * 論理削除強制 Repository 基底クラスの有効化。
 *
 * <p>{@code repositoryBaseClass} を指定すると、Spring Data JPA が各 Repository インターフェースの実体を生成する際、{@code
 * SimpleJpaRepository} の代わりに {@link SoftDeleteRepositoryImpl} を使うようになる。
 *
 * <p>既存の {@code JpaAuditConfig}（@EnableJpaAuditing）とは別クラスにしているのは、 「監査」と「Repository
 * 生成戦略」という異なる関心事を1つの設定クラスに 同居させないため（config/ パッケージの責務分離の規約に従う）。
 */
@Configuration
@EnableJpaRepositories(
        basePackages = "com.example.ecapi.repository",
        repositoryBaseClass = SoftDeleteRepositoryImpl.class)
public class SoftDeleteJpaConfig {}
