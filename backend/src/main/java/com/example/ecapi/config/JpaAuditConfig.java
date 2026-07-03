package com.example.ecapi.config;

import com.example.ecapi.service.auth.LoginUserDetails;
import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class JpaAuditConfig {

    @Bean
    public AuditorAware<Long> auditorAware() {
        return () ->
                Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                        .filter(Authentication::isAuthenticated)
                        .filter(auth -> auth.getPrincipal() instanceof LoginUserDetails)
                        .map(auth -> ((LoginUserDetails) auth.getPrincipal()).getUserId());
    }
}
