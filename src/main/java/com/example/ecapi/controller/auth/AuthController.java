package com.example.ecapi.controller.auth;

import com.example.ecapi.config.JwtHelper;
import com.example.ecapi.config.JwtProperties;
import com.example.ecapi.controller.auth.dto.*;
import com.example.ecapi.entity.Customer;
import com.example.ecapi.exception.AuthException;
import com.example.ecapi.repository.CustomerRepository;
import com.example.ecapi.service.auth.TokenRedisService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtHelper jwtHelper;
    private final JwtProperties jwtProperties;
    private final TokenRedisService tokenRedisService;
    private final CustomerRepository CustomerRepository;
    private final PasswordEncoder passwordEncoder;

    /** ユーザー登録 */
    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public void signup(@Valid @RequestBody SignupRequest req) {
        if (CustomerRepository.findByEmail(req.email()).isPresent()) {
            throw new AuthException("Email already registered");
        }
        Customer customer = new Customer();
        customer.setEmail(req.email());
        customer.setPassword(passwordEncoder.encode(req.password()));
        customer.setRole("ROLE_Customer");
        CustomerRepository.save(customer);
    }

    /** ログイン → JWT 発行 */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.email(), req.password()));
        } catch (AuthenticationException e) {
            throw new AuthException("Invalid email or password");
        }

        Customer Customer = CustomerRepository.findByEmail(req.email()).orElseThrow();

        String accessToken = jwtHelper.generateAccessToken(Customer.getEmail(), Customer.getRole());
        String refreshToken = jwtHelper.generateRefreshToken(Customer.getEmail());
        String jti = jwtHelper.extractJti(refreshToken);
        long refreshExpiry = (long) jwtProperties.getRefreshTokenExpiryDays() * 86400;

        tokenRedisService.saveRefreshToken(Customer.getEmail(), jti, refreshExpiry);

        return ResponseEntity.ok(
                new LoginResponse(
                        accessToken,
                        refreshToken,
                        "Bearer",
                        jwtProperties.getAccessTokenExpiryMinutes()));
    }

    /** トークン再発行 */
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(@Valid @RequestBody RefreshRequest req) {
        Claims claims;
        try {
            claims = jwtHelper.parseClaims(req.refreshToken());
        } catch (JwtException e) {
            throw new AuthException("Invalid refresh token");
        }

        if (!"refresh".equals(claims.get("type", String.class))) {
            throw new AuthException("Not a refresh token");
        }

        String email = claims.getSubject();
        String jti = claims.getId();

        if (!tokenRedisService.isRefreshTokenValid(email, jti)) {
            throw new AuthException("Refresh token has been revoked or expired");
        }

        Customer Customer = CustomerRepository.findByEmail(email).orElseThrow();

        // 旧トークン無効化・新トークン発行（ローテーション）
        tokenRedisService.deleteRefreshToken(email);
        String newAccessToken = jwtHelper.generateAccessToken(email, Customer.getRole());
        String newRefreshToken = jwtHelper.generateRefreshToken(email);
        String newJti = jwtHelper.extractJti(newRefreshToken);
        long refreshExpiry = (long) jwtProperties.getRefreshTokenExpiryDays() * 86400;
        tokenRedisService.saveRefreshToken(email, newJti, refreshExpiry);

        return ResponseEntity.ok(
                new LoginResponse(
                        newAccessToken,
                        newRefreshToken,
                        "Bearer",
                        jwtProperties.getAccessTokenExpiryMinutes()));
    }

    /** ログアウト（アクセストークンをブラックリストへ） */
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        Claims claims = jwtHelper.parseClaims(token);
        String email = claims.getSubject();
        long remaining = jwtHelper.getRemainingSeconds(token);

        // アクセストークンは jti がないため subject+exp で管理してもよいが、
        // ここでは jti を付与するよう generateAccessToken を拡張済みの場合は blacklist 登録
        tokenRedisService.blacklistToken(
                claims.getId() != null ? claims.getId() : token, remaining);
        tokenRedisService.deleteRefreshToken(email);
    }
}
