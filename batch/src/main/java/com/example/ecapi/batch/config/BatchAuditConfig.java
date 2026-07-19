package com.example.ecapi.batch.config;

import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * バッチにはHTTPセッション/SecurityContextが存在しないため、backendのJpaAuditConfigとは別に
 * 固定のバッチ用システムユーザーIDを返すAuditorAwareを定義する（14.6節参照）。
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class BatchAuditConfig {

    public static final long BATCH_SYSTEM_USER_ID = 0L;

    @Bean
    public AuditorAware<Long> auditorAware() {
        return () -> Optional.of(BATCH_SYSTEM_USER_ID);
    }
}
