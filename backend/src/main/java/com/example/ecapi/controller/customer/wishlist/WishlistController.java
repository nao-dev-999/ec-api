package com.example.ecapi.controller.customer.wishlist;

import com.example.ecapi.controller.customer.wishlist.dto.AddWishlistItemRequest;
import com.example.ecapi.controller.customer.wishlist.dto.WishlistItemResponse;
import com.example.ecapi.service.auth.LoginUserDetails;
import com.example.ecapi.service.wishlist.WishlistService;
import com.example.ecapi.service.wishlist.dto.WishlistItemResult;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * お気に入り（ウィッシュリスト）REST コントローラー（顧客本人のみ、要ログイン）
 *
 * <pre>
 * GET    /api/customer/wishlist               自分のお気に入り一覧
 * POST   /api/customer/wishlist/items          お気に入りに追加（登録済みの場合は既存の登録を返す、冪等）
 * DELETE /api/customer/wishlist/items/{productId}  お気に入りから削除（未登録でもエラーにしない、冪等）
 * </pre>
 */
@RestController
@RequestMapping("/api/customer/wishlist")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

    @GetMapping
    public ResponseEntity<List<WishlistItemResponse>> getWishlist(
            @AuthenticationPrincipal LoginUserDetails loginUser) {
        return ResponseEntity.ok(
                wishlistService.getWishlist(loginUser.getUserId()).stream()
                        .map(this::toResponse)
                        .toList());
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
