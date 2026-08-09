package com.example.ecapi.controller.admin.coupon;

import com.example.ecapi.controller.admin.coupon.dto.AdminCouponResponse;
import com.example.ecapi.controller.admin.coupon.dto.CreateCouponRequest;
import com.example.ecapi.controller.admin.coupon.dto.UpdateCouponRequest;
import com.example.ecapi.controller.common.dto.PageResponse;
import com.example.ecapi.service.coupon.CouponService;
import com.example.ecapi.service.coupon.dto.CouponResult;
import com.example.ecapi.service.coupon.dto.CreateCoupon;
import com.example.ecapi.service.coupon.dto.UpdateCoupon;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * クーポン管理 REST コントローラー（管理者向け）
 *
 * <pre>
 * GET    /api/admin/coupons       クーポン一覧を新しい順にページング取得
 * GET    /api/admin/coupons/{id}  クーポン詳細
 * POST   /api/admin/coupons       クーポン作成
 * PUT    /api/admin/coupons/{id}  クーポン更新（部分更新、楽観ロックのため version が必須）
 * DELETE /api/admin/coupons/{id}  クーポン削除
 * </pre>
 */
@RestController
@RequestMapping("/api/admin/coupons")
@RequiredArgsConstructor
public class AdminCouponController {

    private static final int MAX_PAGE_SIZE = 100;

    private final CouponService couponService;

    @GetMapping
    public ResponseEntity<PageResponse<AdminCouponResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable =
                PageRequest.of(
                        Math.max(page, 0),
                        Math.clamp(size, 1, MAX_PAGE_SIZE),
                        Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<AdminCouponResponse> result = couponService.findAll(pageable).map(this::toResponse);
        return ResponseEntity.ok(PageResponse.from(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminCouponResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(toResponse(couponService.findById(id)));
    }

    @PostMapping
    public ResponseEntity<AdminCouponResponse> create(
            @Valid @RequestBody CreateCouponRequest request) {
        CouponResult result =
                couponService.create(
                        new CreateCoupon(
                                request.code(),
                                request.discountAmount(),
                                request.validFrom(),
                                request.validTo(),
                                request.usageLimit()));
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(result));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AdminCouponResponse> update(
            @PathVariable Long id, @Valid @RequestBody UpdateCouponRequest request) {
        CouponResult result =
                couponService.update(
                        new UpdateCoupon(
                                id,
                                request.discountAmount(),
                                request.validFrom(),
                                request.validTo(),
                                request.usageLimit(),
                                request.active(),
                                request.version()));
        return ResponseEntity.ok(toResponse(result));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        couponService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private AdminCouponResponse toResponse(CouponResult result) {
        return new AdminCouponResponse(
                result.id(),
                result.code(),
                result.discountAmount(),
                result.validFrom(),
                result.validTo(),
                result.usageLimit(),
                result.usageCount(),
                result.active(),
                result.createdAt(),
                result.updatedAt(),
                result.version());
    }
}
