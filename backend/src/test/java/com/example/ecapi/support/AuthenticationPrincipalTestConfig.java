package com.example.ecapi.support;

import java.util.List;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * {@code @WebMvcTest} slices only load the target controller (plus anything explicitly
 * {@code @Import}ed), so {@code SecurityConfig}'s {@code @EnableWebSecurity} never runs and {@code
 * WebMvcSecurityConfiguration} (the {@link WebMvcConfigurer} that registers Spring Security's
 * {@link AuthenticationPrincipalArgumentResolver}) never fires. Without it,
 * {@code @AuthenticationPrincipal} parameters silently fall back to Spring MVC's data-binding
 * resolver, producing an all-null object instead of the authenticated principal. Import this config
 * in any {@code @WebMvcTest} for a controller using {@code @AuthenticationPrincipal}.
 */
@TestConfiguration
public class AuthenticationPrincipalTestConfig implements WebMvcConfigurer {

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new AuthenticationPrincipalArgumentResolver());
    }
}
