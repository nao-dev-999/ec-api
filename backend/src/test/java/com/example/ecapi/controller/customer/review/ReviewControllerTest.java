package com.example.ecapi.controller.customer.review;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.ecapi.controller.customer.review.dto.CreateReviewRequest;
import com.example.ecapi.controller.customer.review.dto.UpdateReviewRequest;
import com.example.ecapi.exception.GlobalExceptionHandler;
import com.example.ecapi.exception.ReviewAlreadyExistsException;
import com.example.ecapi.exception.ReviewNotAllowedException;
import com.example.ecapi.exception.ReviewNotFoundException;
import com.example.ecapi.helper.MessageHelper;
import com.example.ecapi.service.review.ReviewService;
import com.example.ecapi.service.review.dto.CreateReview;
import com.example.ecapi.service.review.dto.ReviewResult;
import com.example.ecapi.service.review.dto.UpdateReview;
import com.example.ecapi.support.AuthenticationPrincipalTestConfig;
import com.example.ecapi.support.WithMockLoginUser;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

@WebMvcTest(ReviewController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, AuthenticationPrincipalTestConfig.class})
class ReviewControllerTest {

    @MockitoBean private ReviewService reviewService;
    @MockitoBean private MessageHelper messageHelper;
    @Autowired private JsonMapper jsonMapper;
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
    @DisplayName("POST /api/customer/reviews")
    @WithMockLoginUser
    class CreateTest {

        @Test
        @DisplayName("レビューを投稿できること")
        void shouldCreateReview() throws Exception {
            CreateReviewRequest request = new CreateReviewRequest(10L, 5, "最高");
            when(reviewService.create(any(CreateReview.class))).thenReturn(reviewResult);

            mockMvc.perform(
                            post("/api/customer/reviews")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.rating").value(5));
        }

        @Test
        @DisplayName("バリデーションエラーの場合、400を返すこと")
        void shouldReturnBadRequestWhenValidationFails() throws Exception {
            CreateReviewRequest invalidRequest = new CreateReviewRequest(null, 6, null);

            mockMvc.perform(
                            post("/api/customer/reviews")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(jsonMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("配送完了済みの購入実績がない場合、409を返すこと")
        void shouldReturnConflictWhenNotAllowed() throws Exception {
            CreateReviewRequest request = new CreateReviewRequest(10L, 5, "最高");
            doThrow(new ReviewNotAllowedException(10L))
                    .when(reviewService)
                    .create(any(CreateReview.class));

            mockMvc.perform(
                            post("/api/customer/reviews")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("すでにレビュー済みの場合、409を返すこと")
        void shouldReturnConflictWhenAlreadyExists() throws Exception {
            CreateReviewRequest request = new CreateReviewRequest(10L, 5, "最高");
            doThrow(new ReviewAlreadyExistsException(10L))
                    .when(reviewService)
                    .create(any(CreateReview.class));

            mockMvc.perform(
                            post("/api/customer/reviews")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("PUT /api/customer/reviews/{id}")
    @WithMockLoginUser
    class UpdateTest {

        @Test
        @DisplayName("自分のレビューを更新できること")
        void shouldUpdateReview() throws Exception {
            UpdateReviewRequest request = new UpdateReviewRequest(3, "普通でした", 0);
            when(reviewService.update(any(UpdateReview.class))).thenReturn(reviewResult);

            mockMvc.perform(
                            put("/api/customer/reviews/{id}", 100L)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(reviewResult.id()));
        }

        @Test
        @DisplayName("他人のレビューを指定した場合、404を返すこと")
        void shouldReturnNotFoundWhenNotOwnReview() throws Exception {
            UpdateReviewRequest request = new UpdateReviewRequest(3, "普通でした", 0);
            doThrow(new ReviewNotFoundException(99L))
                    .when(reviewService)
                    .update(any(UpdateReview.class));

            mockMvc.perform(
                            put("/api/customer/reviews/{id}", 99L)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /api/customer/reviews/{id}")
    @WithMockLoginUser
    class DeleteTest {

        @Test
        @DisplayName("自分のレビューを削除できること")
        void shouldDeleteReview() throws Exception {
            doNothing().when(reviewService).delete(anyLong(), anyLong());

            mockMvc.perform(delete("/api/customer/reviews/{id}", 100L))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("他人のレビューを指定した場合、404を返すこと")
        void shouldReturnNotFoundWhenNotOwnReview() throws Exception {
            doThrow(new ReviewNotFoundException(99L))
                    .when(reviewService)
                    .delete(anyLong(), anyLong());

            mockMvc.perform(delete("/api/customer/reviews/{id}", 99L))
                    .andExpect(status().isNotFound());
        }
    }
}
