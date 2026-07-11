package com.example.ecapi.config;

import com.example.ecapi.filter.RequestLoggingFilter;
import com.example.ecapi.filter.RequestTracingFilter;
import com.example.ecapi.helper.MessageHelper;
import com.example.ecapi.service.auth.CustomerUserDetailsService;
import com.example.ecapi.service.auth.UserDetailsServiceImpl;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final MessageHelper messageHelper;

    @Value("${app.cors.allowed-origins:http://localhost:3000}")
    private List<String> allowedOrigins;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(
                        sm -> sm.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(
                        auth ->
                                auth
                                        // Swagger UI を認証不要に追加
                                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**")
                                        .permitAll()
                                        // ALB/ECSのヘルスチェック
                                        .requestMatchers("/actuator/health/**")
                                        .permitAll()
                                        .requestMatchers("/api/auth/**")
                                        .permitAll()
                                        // 商品参照は全員可（作成・更新・削除は ADMIN のみ）
                                        .requestMatchers("/api/customer/products/**")
                                        .permitAll()
                                        .requestMatchers("/api/customer/auth/**")
                                        .permitAll()
                                        .requestMatchers("/api/customer/cart/**")
                                        .hasRole("CUSTOMER")
                                        .requestMatchers("/api/customer/me/**")
                                        .hasRole("CUSTOMER")
                                        .requestMatchers("/api/orders/**")
                                        .hasRole("CUSTOMER")
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

        http.addFilterBefore(new RequestTracingFilter(), SecurityContextHolderFilter.class);
        http.addFilterAfter(new RequestLoggingFilter(), RequestTracingFilter.class);

        return http.build();
    }

    private CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(
                List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    @Primary
    public AuthenticationManager authenticationManager(
            UserDetailsServiceImpl userDetailsService, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    @Bean
    public HttpSessionSecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean("customerAuthenticationManager")
    public AuthenticationManager customerAuthenticationManager(
            CustomerUserDetailsService customerUserDetailsService,
            PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(customerUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }
}
