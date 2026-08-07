package com.example.ecapi.filter;

import com.example.ecapi.helper.MessageHelper;
import com.example.ecapi.service.auth.LoginUserDetails;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * IPアドレス・ユーザー単位でリクエスト数を制限する（Bucket4j + Redis、複数インスタンス間で共有）。 WAF（AWS
 * WAFv2レートベースルール）が縁で粗い足切りを行い、本フィルターはエンドポイント単位の細かい制限を担う。
 */
public class RateLimitingFilter extends OncePerRequestFilter {

    private final ProxyManager<byte[]> proxyManager;
    private final MessageHelper messageHelper;

    public RateLimitingFilter(ProxyManager<byte[]> proxyManager, MessageHelper messageHelper) {
        this.proxyManager = proxyManager;
        this.messageHelper = messageHelper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        LimitTier tier = LimitTier.resolve(request.getRequestURI());
        String key = tier.name() + ":" + resolveClientKey(request);
        Bucket bucket =
                proxyManager
                        .builder()
                        .build(key.getBytes(StandardCharsets.UTF_8), tier::configuration);

        if (!bucket.tryConsume(1)) {
            response.setStatus(429);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter()
                    .write(
                            """
                            {"status":429,"error":"Too Many Requests","message":"%s"}
                            """
                                    .formatted(messageHelper.get("error.tooManyRequests")));
            return;
        }
        chain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs");
    }

    private String resolveClientKey(HttpServletRequest request) {
        // 認証済みユーザはユーザID単位、未認証はIPアドレス単位で制限
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .filter(Authentication::isAuthenticated)
                .filter(a -> a.getPrincipal() instanceof LoginUserDetails)
                .map(a -> "user:" + ((LoginUserDetails) a.getPrincipal()).getUserId())
                .orElse("ip:" + request.getRemoteAddr());
    }

    private enum LimitTier {
        // ブルートフォース攻撃の防止
        LOGIN(10, Duration.ofMinutes(1)),
        // スパムアカウント作成の防止
        SIGNUP(5, Duration.ofHours(1)),
        // 一般的な過負荷対策
        GENERAL(200, Duration.ofMinutes(1));

        private final long capacity;
        private final Duration period;

        LimitTier(long capacity, Duration period) {
            this.capacity = capacity;
            this.period = period;
        }

        BucketConfiguration configuration() {
            return BucketConfiguration.builder()
                    .addLimit(
                            Bandwidth.builder()
                                    .capacity(capacity)
                                    .refillGreedy(capacity, period)
                                    .build())
                    .build();
        }

        static LimitTier resolve(String path) {
            if (path.contains("/auth/login")) {
                return LOGIN;
            }
            if (path.contains("/auth/signup")) {
                return SIGNUP;
            }
            return GENERAL;
        }
    }
}
