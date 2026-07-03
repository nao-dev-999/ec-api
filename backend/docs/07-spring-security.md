# 7. Spring Security

> [← インデックスに戻る](../コーディング規約.md)

---

## SecurityConfig の基本方針

```java
// 認可: hasAnyAuthority() を使う（hasAnyRole()はROLE_プレフィックスが二重になるリスクがある）
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/api/admin/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_PRODUCT_MANAGER")
    .requestMatchers("/api/customer/**").hasAuthority("ROLE_CUSTOMER")
    .requestMatchers("/api/auth/**").permitAll()
    .anyRequest().authenticated()
)
```

---

## メソッドセキュリティ

- クラスレベルの `@PreAuthorize` でロールガードをかける
- メソッドレベルのオーナーチェック（自分の注文のみ操作可能等）には `@PostAuthorize` または Service 内で `customerId` を比較する

```java
// ✅ Controller クラスレベルでロール制限
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
@RestController
public class AdminOrderController { ... }

// ✅ @EnableMethodSecurity が有効であることを SecurityConfig で確認
@Configuration
@EnableMethodSecurity
public class SecurityConfig { ... }
```

---

## セッション管理（Redis）

- JWT は使用しない（`JwtAuthenticationFilter`, `JwtHelper`, `JwtProperties`, `TokenRedisService` は削除済み）
- Spring Session + Redis でセッション管理
- CSRF は REST API（`Content-Type: application/json` + CORS制御）では無効化可
- Cookie は `SameSite=Lax` で CSRF 保護
