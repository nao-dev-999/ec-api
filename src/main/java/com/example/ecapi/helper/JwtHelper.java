package com.example.ecapi.helper;

import com.example.ecapi.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtHelper {

    private final JwtProperties jwtProperties;

    private SecretKey key() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    /** アクセストークン生成 */
    public String generateAccessToken(String email, String role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(email)
                .claim("role", role)
                .claim("type", "access")
                .issuedAt(Date.from(now))
                .expiration(
                        Date.from(
                                now.plusSeconds(jwtProperties.getAccessTokenExpiryMinutes() * 60L)))
                .signWith(key())
                .compact();
    }

    /** リフレッシュトークン生成（jti でブラックリスト管理） */
    public String generateRefreshToken(String email) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(email)
                .claim("type", "refresh")
                .id(UUID.randomUUID().toString()) // jti
                .issuedAt(Date.from(now))
                .expiration(
                        Date.from(
                                now.plusSeconds(
                                        jwtProperties.getRefreshTokenExpiryDays() * 86400L)))
                .signWith(key())
                .compact();
    }

    /** トークンのクレームを検証・取得 */
    public Claims parseClaims(String token) {
        return Jwts.parser().verifyWith(key()).build().parseSignedClaims(token).getPayload();
    }

    public String extractEmail(String token) {
        return parseClaims(token).getSubject();
    }

    public String extractJti(String token) {
        return parseClaims(token).getId();
    }

    public long getRemainingSeconds(String token) {
        Date exp = parseClaims(token).getExpiration();
        long remaining = (exp.getTime() - System.currentTimeMillis()) / 1000;
        return Math.max(remaining, 0);
    }
}
