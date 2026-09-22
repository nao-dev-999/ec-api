package com.example.ecapi.controller.customer.product;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.ecapi.exception.GlobalExceptionHandler;
import com.example.ecapi.exception.ProductNotFoundException;
import com.example.ecapi.helper.MessageHelper;
import com.example.ecapi.service.review.ReviewService;
import com.example.ecapi.service.review.dto.ReviewResult;
import com.example.ecapi.service.review.dto.ReviewSummaryResult;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProductReviewController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class})
class ProductReviewControllerTest {

    @MockitoBean private ReviewService reviewService;
    @MockitoBean private MessageHelper messageHelper;
    @Autowired private MockMvc mockMvc;

    private ReviewResult reviewResult;

    @BeforeEach
    void setUp() {
        reviewResult =
                new ReviewResult(
                        100L,
                        10L,
                        "Test Product",
                        1L,
                        "山田",
                        5,
                        "最高",
                        Instant.now(),
                        Instant.now(),
                        0);
    }

    @Nested
    @DisplayName("GET /api/customer/products/{productId}/reviews")
    class ListByProductTest {

        @Test
        @DisplayName("商品のレビュー一覧・平均評価を取得できること")
        void shouldReturnReviewsAndSummary() throws Exception {
            Page<ReviewResult> page =
                    new PageImpl<>(List.of(reviewResult), PageRequest.of(0, 20), 1);
            when(reviewService.listByProduct(eq(10L), any())).thenReturn(page);
            when(reviewService.getSummary(10L)).thenReturn(new ReviewSummaryResult(5.0, 1L));

            mockMvc.perform(get("/api/customer/products/{productId}/reviews", 10L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.reviews.content[0].id").value(reviewResult.id()))
                    .andExpect(jsonPath("$.reviews.totalElements").value(1))
                    .andExpect(jsonPath("$.summary.averageRating").value(5.0))
                    .andExpect(jsonPath("$.summary.reviewCount").value(1));
        }

        @Test
        @DisplayName("レビューが0件の場合、空のリストと平均評価0を返すこと")
        void shouldReturnEmptyWhenNoReviews() throws Exception {
            Page<ReviewResult> page =
                    new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 20), 0);
            when(reviewService.listByProduct(eq(10L), any())).thenReturn(page);
            when(reviewService.getSummary(10L)).thenReturn(new ReviewSummaryResult(0.0, 0L));

            mockMvc.perform(get("/api/customer/products/{productId}/reviews", 10L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.reviews.content").isEmpty())
                    .andExpect(jsonPath("$.summary.reviewCount").value(0));
        }

        @Test
        @DisplayName("存在しない商品の場合、404を返すこと")
        void shouldReturnNotFoundWhenProductDoesNotExist() throws Exception {
            when(reviewService.listByProduct(eq(99L), any()))
                    .thenThrow(new ProductNotFoundException(99L));

            mockMvc.perform(get("/api/customer/products/{productId}/reviews", 99L))
                    .andExpect(status().isNotFound());
        }
    }
}
