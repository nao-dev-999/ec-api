package com.example.ecapi.controller.admin.coupon;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.ecapi.controller.admin.coupon.dto.CreateCouponRequest;
import com.example.ecapi.controller.admin.coupon.dto.UpdateCouponRequest;
import com.example.ecapi.exception.CouponCodeDuplicateException;
import com.example.ecapi.exception.CouponNotFoundException;
import com.example.ecapi.exception.GlobalExceptionHandler;
import com.example.ecapi.helper.MessageHelper;
import com.example.ecapi.service.coupon.CouponService;
import com.example.ecapi.service.coupon.dto.CouponResult;
import com.example.ecapi.service.coupon.dto.CreateCoupon;
import com.example.ecapi.service.coupon.dto.UpdateCoupon;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

@WebMvcTest(AdminCouponController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class})
class AdminCouponControllerTest {

    @MockitoBean private CouponService couponService;
    @MockitoBean private MessageHelper messageHelper;
    @Autowired private JsonMapper jsonMapper;
    @Autowired private MockMvc mockMvc;

    private CouponResult couponResult;

    @BeforeEach
    void setUp() {
        couponResult =
                new CouponResult(
                        1L,
                        "SUMMER500",
                        BigDecimal.valueOf(500),
                        LocalDateTime.now(),
                        LocalDateTime.now().plusDays(30),
                        100,
                        0,
                        true,
                        LocalDateTime.now(),
                        LocalDateTime.now(),
                        0);
    }

    @Nested
    @DisplayName("GET /api/admin/coupons")
    class GetAllTest {

        @Test
        @DisplayName("クーポン一覧を取得できること")
        void shouldGetAllCoupons() throws Exception {
            when(couponService.findAll(any()))
                    .thenReturn(new PageImpl<>(List.of(couponResult), PageRequest.of(0, 20), 1));

            mockMvc.perform(get("/api/admin/coupons"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(couponResult.id()))
                    .andExpect(jsonPath("$.content[0].code").value(couponResult.code()));
        }
    }

    @Nested
    @DisplayName("GET /api/admin/coupons/{id}")
    class GetByIdTest {

        @Test
        @DisplayName("指定したIDのクーポンを取得できること")
        void shouldGetCouponById() throws Exception {
            when(couponService.findById(1L)).thenReturn(couponResult);

            mockMvc.perform(get("/api/admin/coupons/{id}", 1L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(couponResult.code()));
        }

        @Test
        @DisplayName("指定したIDのクーポンが見つからない場合、404を返すこと")
        void shouldReturnNotFoundWhenCouponDoesNotExist() throws Exception {
            doThrow(new CouponNotFoundException(99L)).when(couponService).findById(99L);

            mockMvc.perform(get("/api/admin/coupons/{id}", 99L)).andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/admin/coupons")
    class CreateTest {

        @Test
        @DisplayName("クーポンを新規登録できること")
        void shouldCreateCoupon() throws Exception {
            CreateCouponRequest request =
                    new CreateCouponRequest(
                            "SUMMER500",
                            BigDecimal.valueOf(500),
                            LocalDateTime.now(),
                            LocalDateTime.now().plusDays(30),
                            100);
            when(couponService.create(any(CreateCoupon.class))).thenReturn(couponResult);

            mockMvc.perform(
                            post("/api/admin/coupons")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.code").value(couponResult.code()));
        }

        @Test
        @DisplayName("バリデーションエラーの場合、400を返すこと")
        void shouldReturnBadRequestWhenValidationFails() throws Exception {
            CreateCouponRequest invalidRequest =
                    new CreateCouponRequest("", null, null, null, null);

            mockMvc.perform(
                            post("/api/admin/coupons")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(jsonMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("クーポンコードが重複している場合、409を返すこと")
        void shouldReturnConflictWhenCodeDuplicate() throws Exception {
            CreateCouponRequest request =
                    new CreateCouponRequest("SUMMER500", BigDecimal.valueOf(500), null, null, null);
            doThrow(new CouponCodeDuplicateException("SUMMER500"))
                    .when(couponService)
                    .create(any(CreateCoupon.class));

            mockMvc.perform(
                            post("/api/admin/coupons")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("PUT /api/admin/coupons/{id}")
    class UpdateTest {

        @Test
        @DisplayName("クーポンを更新できること")
        void shouldUpdateCoupon() throws Exception {
            UpdateCouponRequest request =
                    new UpdateCouponRequest(BigDecimal.valueOf(1000), null, null, null, false, 0);
            when(couponService.update(any(UpdateCoupon.class))).thenReturn(couponResult);

            mockMvc.perform(
                            put("/api/admin/coupons/{id}", 1L)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(couponResult.id()));
        }

        @Test
        @DisplayName("指定したIDのクーポンが見つからない場合、404を返すこと")
        void shouldReturnNotFoundWhenCouponDoesNotExist() throws Exception {
            UpdateCouponRequest request =
                    new UpdateCouponRequest(BigDecimal.valueOf(1000), null, null, null, false, 0);
            doThrow(new CouponNotFoundException(99L))
                    .when(couponService)
                    .update(any(UpdateCoupon.class));

            mockMvc.perform(
                            put("/api/admin/coupons/{id}", 99L)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /api/admin/coupons/{id}")
    class DeleteTest {

        @Test
        @DisplayName("クーポンを削除できること")
        void shouldDeleteCoupon() throws Exception {
            doNothing().when(couponService).delete(1L);

            mockMvc.perform(delete("/api/admin/coupons/{id}", 1L))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("指定したIDのクーポンが見つからない場合、404を返すこと")
        void shouldReturnNotFoundWhenDeletingNonExistentCoupon() throws Exception {
            doThrow(new CouponNotFoundException(99L)).when(couponService).delete(99L);

            mockMvc.perform(delete("/api/admin/coupons/{id}", 99L))
                    .andExpect(status().isNotFound());
        }
    }
}
