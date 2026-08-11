package com.example.ecapi.config;

import com.example.ecapi.filter.RateLimitingFilter;
import com.example.ecapi.filter.RequestLoggingFilter;
import com.example.ecapi.filter.RequestTracingFilter;
import com.example.ecapi.helper.MessageHelper;
import com.example.ecapi.service.auth.CustomerUserDetailsService;
import com.example.ecapi.service.auth.UserDetailsServiceImpl;
import io.github.bucket4j.distributed.proxy.ProxyManager;
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

    private static final String CONTENT_TYPE_JSON = "application/json;charset=UTF-8";
    private static final String ROLE_CUSTOMER = "CUSTOMER";

    private final MessageHelper messageHelper;
    private final ProxyManager<byte[]> rateLimitProxyManager;

    @Value("${app.cors.allowed-origins:http://localhost:3000}")
    private List<String> allowedOrigins;

    // 負荷試験でレートリミッターの制約を切り離して純粋なAPI/DB性能を計測したい場合、
    // 試験対象環境でのみ APP_RATE_LIMITING_ENABLED=false を設定する(本番では変更しないこと)。
    @Value("${app.rate-limiting.enabled:true}")
    private boolean rateLimitingEnabled;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // CSRF対策はCookie(same-site=Lax、application.yml)と許可オリジンを絞ったCORSで代替。
                // クロスサイトのフォーム送信ではセッションCookieが付与されず、
                // 許可外オリジンからのcredentials付きJSONリクエストはCORSでブロックされる。
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
                                        .hasRole(ROLE_CUSTOMER)
                                        .requestMatchers("/api/customer/me/**")
                                        .hasRole(ROLE_CUSTOMER)
                                        // レビューの参照(/api/customer/products/{id}/reviews)は
                                        // 上の /api/customer/products/** ルールで既に permitAll。
                                        // 投稿・編集・削除のみここでCUSTOMER必須にする。
                                        .requestMatchers("/api/customer/reviews/**")
                                        .hasRole(ROLE_CUSTOMER)
                                        .requestMatchers("/api/customer/wishlist/**")
                                        .hasRole(ROLE_CUSTOMER)
                                        .requestMatchers("/api/customer/coupons/**")
                                        .hasRole(ROLE_CUSTOMER)
                                        .requestMatchers("/api/customer/shipping-addresses/**")
                                        .hasRole(ROLE_CUSTOMER)
                                        .requestMatchers("/api/orders/**")
                                        .hasRole(ROLE_CUSTOMER)
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
                                                    response.setContentType(CONTENT_TYPE_JSON);
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
                                                    response.setContentType(CONTENT_TYPE_JSON);
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
        // ✅ レート制限は認証フィルターより前（認証コストをかける前に弾く）
        // RequestTracingFilter.class を参照するため、上のaddFilterBeforeより後で呼ぶ必要がある
        // （Spring Securityのフィルター順序解決は、参照先クラスが先に登録済みであることを要求する）
        if (rateLimitingEnabled) {
            http.addFilterBefore(
                    new RateLimitingFilter(rateLimitProxyManager, messageHelper),
                    RequestTracingFilter.class);
        }
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
