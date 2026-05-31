package com.example.ecapi.config;

import com.example.ecapi.filter.JwtAuthenticationFilter;
import com.example.ecapi.helper.MessageHelper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final MessageHelper messageHelper;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(
                        auth ->
                                auth
                                        // 認証不要
                                        .requestMatchers("/api/auth/**")
                                        .permitAll()
                                        // 商品参照は全員可（作成・更新・削除は ADMIN のみ）
                                        .requestMatchers(HttpMethod.GET, "/api/products/**")
                                        .permitAll()
                                        .requestMatchers(HttpMethod.POST, "/api/products/**")
                                        .hasRole("ADMIN")
                                        .requestMatchers(HttpMethod.PUT, "/api/products/**")
                                        .hasRole("ADMIN")
                                        .requestMatchers(HttpMethod.DELETE, "/api/products/**")
                                        .hasRole("ADMIN")
                                        // 注文は認証済みユーザーのみ
                                        .requestMatchers("/api/orders/**")
                                        .authenticated()
                                        .anyRequest()
                                        .authenticated())
                .addFilterBefore(
                        jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(
                        ex ->
                                ex.authenticationEntryPoint(
                                                (request, response, authException) -> {
                                                    // トークンなし・無効 → 401
                                                    response.setStatus(
                                                            HttpServletResponse.SC_UNAUTHORIZED);
                                                    response.setContentType(
                                                            "application/json;charset=UTF-8");
                                                    response.getWriter()
                                                            .write(
                                                                    """
                {"status":401,"error":"Unauthorized","message":"%s"}
                """
                                                                            .formatted(
                                                                                    messageHelper
                                                                                            .get(
                                                                                                    "error.unauthorized")));
                                                })
                                        .accessDeniedHandler(
                                                (request, response, accessDeniedException) -> {
                                                    // 権限不足 → 403
                                                    response.setStatus(
                                                            HttpServletResponse.SC_FORBIDDEN);
                                                    response.setContentType(
                                                            "application/json;charset=UTF-8");
                                                    response.getWriter()
                                                            .write(
                                                                    """
                {"status":403,"error":"Forbidden","message":"%s"}
                """
                                                                            .formatted(
                                                                                    messageHelper
                                                                                            .get(
                                                                                                    "error.forbidden")));
                                                }));

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }
}
