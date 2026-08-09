package com.example.ecapi.controller.customer.product;

import com.example.ecapi.controller.customer.product.dto.ProductReviewsResponse;
import com.example.ecapi.controller.customer.product.dto.ReviewResponse;
import com.example.ecapi.controller.customer.product.dto.ReviewSummaryResponse;
import com.example.ecapi.service.review.ReviewService;
import com.example.ecapi.service.review.dto.ReviewResult;
import com.example.ecapi.service.review.dto.ReviewSummaryResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商品詳細画面向けレビュー参照 REST コントローラー
 *
 * <p>{@code /api/customer/products/**} 配下は認証不要（{@link
 * com.example.ecapi.config.SecurityConfig}）。レビューの投稿・編集・削除は {@link
 * com.example.ecapi.controller.customer.review.ReviewController}（要ログイン）を参照。
 *
 * <pre>
 * GET /api/customer/products/{productId}/reviews   指定商品のレビュー一覧・平均評価
 * </pre>
 */
@RestController
@RequestMapping("/api/customer/products/{productId}/reviews")
@RequiredArgsConstructor
public class ProductReviewController {

    private final ReviewService reviewService;

    @GetMapping
    public ResponseEntity<ProductReviewsResponse> listByProduct(@PathVariable Long productId) {
        var reviews = reviewService.listByProduct(productId).stream().map(this::toResponse).toList();
        ReviewSummaryResult summary = reviewService.getSummary(productId);
        return ResponseEntity.ok(
                new ProductReviewsResponse(
                        reviews,
                        new ReviewSummaryResponse(summary.averageRating(), summary.reviewCount())));
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
