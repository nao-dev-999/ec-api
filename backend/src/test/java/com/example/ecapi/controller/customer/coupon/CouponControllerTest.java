package com.example.ecapi.controller.customer.coupon;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.ecapi.exception.CouponNotAllowedException;
import com.example.ecapi.exception.CouponNotFoundException;
import com.example.ecapi.exception.GlobalExceptionHandler;
import com.example.ecapi.helper.MessageHelper;
import com.example.ecapi.service.coupon.CouponService;
import com.example.ecapi.support.AuthenticationPrincipalTestConfig;
import com.example.ecapi.support.WithMockLoginUser;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CouponController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, AuthenticationPrincipalTestConfig.class})
class CouponControllerTest {

    @MockitoBean private CouponService couponService;
    @MockitoBean private MessageHelper messageHelper;
    @Autowired private MockMvc mockMvc;

    @Nested
    @DisplayName("GET /api/customer/coupons/{code}/preview")
    class PreviewTest {

        @Test
        @WithMockLoginUser
        @DisplayName("有効なクーポンの場合、割引額を返すこと")
        void shouldReturnDiscountAmount() throws Exception {
            when(couponService.preview(eq("SAVE500"), anyLong(), any(BigDecimal.class)))
                    .thenReturn(BigDecimal.valueOf(500));

            mockMvc.perform(
                            get("/api/customer/coupons/{code}/preview", "SAVE500")
                                    .param("subtotal", "1000"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SAVE500"))
                    .andExpect(jsonPath("$.discountAmount").value(500));
        }

        @Test
        @WithMockLoginUser
        @DisplayName("存在しないコードの場合、404を返すこと")
        void shouldReturnNotFoundWhenCodeNotFound() throws Exception {
            when(couponService.preview(eq("INVALID"), anyLong(), any(BigDecimal.class)))
                    .thenThrow(new CouponNotFoundException("INVALID"));

            mockMvc.perform(
                            get("/api/customer/coupons/{code}/preview", "INVALID")
                                    .param("subtotal", "1000"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockLoginUser
        @DisplayName("利用できないクーポンの場合、409を返すこと")
        void shouldReturnConflictWhenNotAllowed() throws Exception {
            when(couponService.preview(eq("EXPIRED"), anyLong(), any(BigDecimal.class)))
                    .thenThrow(new CouponNotAllowedException("EXPIRED"));

            mockMvc.perform(
                            get("/api/customer/coupons/{code}/preview", "EXPIRED")
                                    .param("subtotal", "1000"))
                    .andExpect(status().isConflict());
        }
    }
}
