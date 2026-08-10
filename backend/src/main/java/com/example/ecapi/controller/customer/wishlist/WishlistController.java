package com.example.ecapi.controller.customer.wishlist;

import com.example.ecapi.controller.common.dto.PageResponse;
import com.example.ecapi.controller.customer.wishlist.dto.AddWishlistItemRequest;
import com.example.ecapi.controller.customer.wishlist.dto.WishlistItemResponse;
import com.example.ecapi.service.auth.LoginUserDetails;
import com.example.ecapi.service.wishlist.WishlistService;
import com.example.ecapi.service.wishlist.dto.WishlistItemResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * お気に入り（ウィッシュリスト）REST コントローラー（顧客本人のみ、要ログイン）
 *
 * <pre>
 * GET    /api/customer/wishlist                     自分のお気に入り一覧（ページング、新しい順）
 * GET    /api/customer/wishlist/items/{productId}    指定商品の登録状況を確認（未登録の場合404）
 * POST   /api/customer/wishlist/items                お気に入りに追加（登録済みの場合は既存の登録を返す、冪等）
 * DELETE /api/customer/wishlist/items/{productId}    お気に入りから削除（未登録でもエラーにしない、冪等）
 * </pre>
 */
@RestController
@RequestMapping("/api/customer/wishlist")
@RequiredArgsConstructor
public class WishlistController {

    private static final int MAX_PAGE_SIZE = 100;

    private final WishlistService wishlistService;

    @GetMapping
    public ResponseEntity<PageResponse<WishlistItemResponse>> getWishlist(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal LoginUserDetails loginUser) {
        Pageable pageable =
                PageRequest.of(
                        Math.max(page, 0),
                        Math.clamp(size, 1, MAX_PAGE_SIZE),
                        Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<WishlistItemResponse> result =
                wishlistService.getWishlist(loginUser.getUserId(), pageable).map(this::toResponse);
        return ResponseEntity.ok(PageResponse.from(result));
    }

    @GetMapping("/items/{productId}")
    public ResponseEntity<WishlistItemResponse> getItem(
            @AuthenticationPrincipal LoginUserDetails loginUser, @PathVariable Long productId) {
        WishlistItemResult result = wishlistService.getItem(loginUser.getUserId(), productId);
        return ResponseEntity.ok(toResponse(result));
    }

    @PostMapping("/items")
    public ResponseEntity<WishlistItemResponse> addItem(
            @AuthenticationPrincipal LoginUserDetails loginUser,
            @Valid @RequestBody AddWishlistItemRequest request) {
        WishlistItemResult result =
                wishlistService.addItem(loginUser.getUserId(), request.productId());
        return ResponseEntity.ok(toResponse(result));
    }

    @DeleteMapping("/items/{productId}")
    public ResponseEntity<Void> removeItem(
            @AuthenticationPrincipal LoginUserDetails loginUser, @PathVariable Long productId) {
        wishlistService.removeItem(loginUser.getUserId(), productId);
        return ResponseEntity.noContent().build();
    }

    private WishlistItemResponse toResponse(WishlistItemResult result) {
        return new WishlistItemResponse(
                result.id(),
                result.productId(),
                result.productName(),
                result.price(),
                result.stock(),
                result.createdAt());
    }
}
