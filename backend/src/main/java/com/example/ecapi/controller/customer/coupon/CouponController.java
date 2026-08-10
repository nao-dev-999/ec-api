package com.example.ecapi.controller.customer.coupon;

import com.example.ecapi.controller.customer.coupon.dto.CouponPreviewResponse;
import com.example.ecapi.service.auth.LoginUserDetails;
import com.example.ecapi.service.coupon.CouponService;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * クーポン事前検証 REST コントローラー（顧客本人向け、要ログイン）
 *
 * <p>カート画面等で、注文を確定する前にクーポンコードの有効性と割引額を確認するために使用する。 実際の適用（利用回数の加算）は {@code POST /api/orders}
 * で注文を作成する際に行われる ため、ここで有効と判定されても、注文作成時点までに利用上限に達していた場合は失敗しうる。
 *
 * <pre>
 * GET /api/customer/coupons/{code}/preview?subtotal=1000  クーポン適用時の割引額を確認（要CUSTOMER）
 * </pre>
 */
@RestController
@RequestMapping("/api/customer/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @GetMapping("/{code}/preview")
    public ResponseEntity<CouponPreviewResponse> preview(
            @PathVariable String code,
            @RequestParam BigDecimal subtotal,
            @AuthenticationPrincipal LoginUserDetails loginUser) {
        BigDecimal discountAmount = couponService.preview(code, loginUser.getUserId(), subtotal);
        return ResponseEntity.ok(new CouponPreviewResponse(code, discountAmount));
    }
}
