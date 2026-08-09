package com.example.ecapi.controller.customer.review;

import com.example.ecapi.controller.customer.product.dto.ReviewResponse;
import com.example.ecapi.controller.customer.review.dto.CreateReviewRequest;
import com.example.ecapi.controller.customer.review.dto.UpdateReviewRequest;
import com.example.ecapi.service.auth.LoginUserDetails;
import com.example.ecapi.service.review.ReviewService;
import com.example.ecapi.service.review.dto.CreateReview;
import com.example.ecapi.service.review.dto.ReviewResult;
import com.example.ecapi.service.review.dto.UpdateReview;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * レビュー投稿・編集・削除 REST コントローラー（顧客本人のみ、要ログイン）
 *
 * <p>{@code /api/customer/reviews/**} は {@link com.example.ecapi.config.SecurityConfig} で
 * {@code CUSTOMER} ロール必須にしている。商品ごとのレビュー参照（認証不要）は {@link
 * com.example.ecapi.controller.customer.product.ProductReviewController} を参照。
 *
 * <pre>
 * POST   /api/customer/reviews       レビュー投稿（配送完了済みの購入実績が必要、1商品1件まで）
 * PUT    /api/customer/reviews/{id}  自分のレビューを編集（楽観ロックのため version が必須）
 * DELETE /api/customer/reviews/{id}  自分のレビューを削除
 * </pre>
 */
@RestController
@RequestMapping("/api/customer/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public ResponseEntity<ReviewResponse> create(
            @AuthenticationPrincipal LoginUserDetails loginUser,
            @Valid @RequestBody CreateReviewRequest request) {
        ReviewResult result =
                reviewService.create(
                        new CreateReview(
                                loginUser.getUserId(),
                                request.productId(),
                                request.rating(),
                                request.comment()));
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(result));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ReviewResponse> update(
            @AuthenticationPrincipal LoginUserDetails loginUser,
            @PathVariable Long id,
            @Valid @RequestBody UpdateReviewRequest request) {
        ReviewResult result =
                reviewService.update(
                        new UpdateReview(
                                id,
                                loginUser.getUserId(),
                                request.rating(),
                                request.comment(),
                                request.version()));
        return ResponseEntity.ok(toResponse(result));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal LoginUserDetails loginUser, @PathVariable Long id) {
        reviewService.delete(id, loginUser.getUserId());
        return ResponseEntity.noContent().build();
    }

    private ReviewResponse toResponse(ReviewResult result) {
        return new ReviewResponse(
                result.id(),
                result.productId(),
                result.customerId(),
                result.customerName(),
                result.rating(),
                result.comment(),
                result.createdAt(),
                result.updatedAt(),
                result.version());
    }
}
