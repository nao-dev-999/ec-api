package com.example.ecapi.controller.admin.review;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.ecapi.exception.GlobalExceptionHandler;
import com.example.ecapi.exception.ReviewNotFoundException;
import com.example.ecapi.helper.MessageHelper;
import com.example.ecapi.service.review.ReviewService;
import com.example.ecapi.service.review.dto.ReviewResult;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminReviewController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class})
class AdminReviewControllerTest {

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
    @DisplayName("GET /api/admin/reviews")
    class GetAllTest {

        @Test
        @DisplayName("全レビューを取得できること")
        void shouldGetAllReviews() throws Exception {
            when(reviewService.listAllForAdmin(any()))
                    .thenReturn(new PageImpl<>(List.of(reviewResult), PageRequest.of(0, 20), 1));

            mockMvc.perform(get("/api/admin/reviews"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(reviewResult.id()))
                    .andExpect(jsonPath("$.content[0].productName").value("Test Product"));
        }
    }

    @Nested
    @DisplayName("DELETE /api/admin/reviews/{id}")
    class DeleteTest {

        @Test
        @DisplayName("レビューを削除できること")
        void shouldDeleteReview() throws Exception {
            doNothing().when(reviewService).deleteByAdmin(100L);

            mockMvc.perform(delete("/api/admin/reviews/{id}", 100L))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("指定したIDのレビューが見つからない場合、404を返すこと")
        void shouldReturnNotFoundWhenReviewDoesNotExist() throws Exception {
            doThrow(new ReviewNotFoundException(99L)).when(reviewService).deleteByAdmin(99L);

            mockMvc.perform(delete("/api/admin/reviews/{id}", 99L))
                    .andExpect(status().isNotFound());
        }
    }
}
