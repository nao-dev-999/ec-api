package com.example.ecapi.service.auth;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Redis を使ったトークン管理サービス。
 *
 * <p>リフレッシュトークン: "rt:{email}" → jti を保存（最新1件のみ有効） ブラックリスト: "bl:{jti}" → "1" を保存（ログアウト済みトークン）
 */
@Service
@RequiredArgsConstructor
public class TokenRedisService {

    private static final String REFRESH_PREFIX = "rt:";
    private static final String BLACKLIST_PREFIX = "bl:";

    private final RedisTemplate<String, String> redisTemplate;

    /** リフレッシュトークンの jti を保存 */
    public void saveRefreshToken(String email, String jti, long expirySeconds) {
        redisTemplate
                .opsForValue()
                .set(REFRESH_PREFIX + email, jti, Duration.ofSeconds(expirySeconds));
    }

    /** 保存済み jti と一致するか検証 */
    public boolean isRefreshTokenValid(String email, String jti) {
        String stored = redisTemplate.opsForValue().get(REFRESH_PREFIX + email);
        return jti.equals(stored);
    }

    /** リフレッシュトークンを削除（ログアウト） */
    public void deleteRefreshToken(String email) {
        redisTemplate.delete(REFRESH_PREFIX + email);
    }

    /** アクセストークンをブラックリストに追加（ログアウト時） */
    public void blacklistToken(String jti, long remainingSeconds) {
        if (remainingSeconds > 0) {
            redisTemplate
                    .opsForValue()
                    .set(BLACKLIST_PREFIX + jti, "1", Duration.ofSeconds(remainingSeconds));
        }
    }

    /** ブラックリスト確認 */
    public boolean isBlacklisted(String jti) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + jti));
    }
}
