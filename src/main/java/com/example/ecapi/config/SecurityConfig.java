package com.example.ecapi.config;

import com.example.ecapi.filter.RequestLoggingFilter;
import com.example.ecapi.filter.RequestTracingFilter;
import com.example.ecapi.helper.MessageHelper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextHolderFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final MessageHelper messageHelper;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(
                        sm -> sm.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(
                        auth ->
                                auth
                                        // Swagger UI を認証不要に追加
                                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**")
                                        .permitAll()
                                        .requestMatchers("/api/auth/**")
                                        .permitAll()
                                        // 商品参照は全員可（作成・更新・削除は ADMIN のみ）
                                        .requestMatchers("/api/customer/products/**")
                                        .permitAll()
                                        .requestMatchers("/api/orders/**")
                                        .authenticated()
                                        .requestMatchers("/api/admin/**")
                                        .hasAnyRole("ADMIN", "PRODUCT_MANAGER")
                                        .anyRequest()
                                        .denyAll())
                .exceptionHandling(
                        ex ->
                                ex.authenticationEntryPoint(
                                                (request, response, authException) -> {
                                                    response.setStatus(
                                                            HttpServletResponse.SC_UNAUTHORIZED);
                                                    response.setContentType(
                                                            "application/json");
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
                                                    response.setStatus(
                                                            HttpServletResponse.SC_FORBIDDEN);
                                                    response.setContentType(
                                                            "application/json");
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

        http.addFilterBefore(new RequestTracingFilter(), SecurityContextHolderFilter.class);
        http.addFilterAfter(new RequestLoggingFilter(), RequestTracingFilter.class);

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

    @Bean
    public HttpSessionSecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }
}
