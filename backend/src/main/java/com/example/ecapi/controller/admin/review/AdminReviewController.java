package com.example.ecapi.controller.admin.review;

import com.example.ecapi.controller.admin.review.dto.AdminReviewResponse;
import com.example.ecapi.controller.common.dto.PageResponse;
import com.example.ecapi.service.review.ReviewService;
import com.example.ecapi.service.review.dto.ReviewResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * レビュー管理 REST コントローラー（管理者向け、不適切投稿のモデレーション目的）
 *
 * <pre>
 * GET    /api/admin/reviews       全レビューを新しい順にページング取得
 * DELETE /api/admin/reviews/{id}  レビューを削除（所有者チェックなし）
 * </pre>
 */
@RestController
@RequestMapping("/api/admin/reviews")
@RequiredArgsConstructor
public class AdminReviewController {

    private static final int MAX_PAGE_SIZE = 100;

    private final ReviewService reviewService;

    @GetMapping
    public ResponseEntity<PageResponse<AdminReviewResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable =
                PageRequest.of(
                        Math.max(page, 0),
                        Math.clamp(size, 1, MAX_PAGE_SIZE),
                        Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<AdminReviewResponse> result =
                reviewService.listAllForAdmin(pageable).map(this::toAdminReviewResponse);
        return ResponseEntity.ok(PageResponse.from(result));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        reviewService.deleteByAdmin(id);
        return ResponseEntity.noContent().build();
    }

    private AdminReviewResponse toAdminReviewResponse(ReviewResult result) {
        return new AdminReviewResponse(
                result.id(),
                result.productId(),
                result.productName(),
                result.customerId(),
                result.customerName(),
                result.rating(),
                result.comment(),
                result.createdAt(),
                result.updatedAt(),
                result.version());
    }
}
