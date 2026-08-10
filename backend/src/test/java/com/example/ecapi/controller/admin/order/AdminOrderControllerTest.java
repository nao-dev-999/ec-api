package com.example.ecapi.controller.admin.order;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.ecapi.constant.OrderStatus;
import com.example.ecapi.exception.GlobalExceptionHandler;
import com.example.ecapi.exception.OrderNotFoundException;
import com.example.ecapi.helper.MessageHelper;
import com.example.ecapi.service.order.OrderService;
import com.example.ecapi.service.order.dto.OrderResult;
import com.example.ecapi.service.order.dto.OrderResultItem;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminOrderController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class})
class AdminOrderControllerTest {

    @MockitoBean private OrderService orderService;
    @MockitoBean private MessageHelper messageHelper;
    @Autowired private MockMvc mockMvc;

    private OrderResult orderResult;

    @BeforeEach
    void setUp() {
        orderResult =
                new OrderResult(
                        1L,
                        10L,
                        "Test Customer",
                        OrderStatus.PENDING,
                        BigDecimal.valueOf(1000),
                        "SAVE500",
                        BigDecimal.valueOf(500),
                        "Test Recipient",
                        "100-0001",
                        "東京都",
                        "千代田区",
                        "1-1-1",
                        null,
                        "090-1111-2222",
                        List.of(
                                new OrderResultItem(
                                        100L,
                                        "Test Product",
                                        2,
                                        BigDecimal.valueOf(500),
                                        BigDecimal.valueOf(1000))),
                        LocalDateTime.now(),
                        LocalDateTime.now(),
                        0);
    }

    @Nested
    @DisplayName("GET /api/admin/orders")
    class GetAllTest {

        @Test
        @DisplayName("全顧客の注文一覧を取得できること")
        void shouldGetAllOrders() throws Exception {
            when(orderService.findAll(any()))
                    .thenReturn(new PageImpl<>(List.of(orderResult), PageRequest.of(0, 20), 1));

            mockMvc.perform(get("/api/admin/orders"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(orderResult.id()))
                    .andExpect(jsonPath("$.content[0].couponCode").value("SAVE500"))
                    .andExpect(jsonPath("$.content[0].discountAmount").value(500));
        }
    }

    @Nested
    @DisplayName("GET /api/admin/orders/{id}")
    class GetByIdTest {

        @Test
        @DisplayName("指定したIDの注文を取得できること")
        void shouldGetOrderById() throws Exception {
            when(orderService.findById(1L)).thenReturn(orderResult);

            mockMvc.perform(get("/api/admin/orders/{id}", 1L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.customerId").value(orderResult.customerId()))
                    .andExpect(jsonPath("$.items[0].productId").value(100));
        }

        @Test
        @DisplayName("指定したIDの注文が見つからない場合、404を返すこと")
        void shouldReturnNotFoundWhenOrderDoesNotExist() throws Exception {
            doThrow(new OrderNotFoundException(99L)).when(orderService).findById(99L);

            mockMvc.perform(get("/api/admin/orders/{id}", 99L)).andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PATCH /api/admin/orders/{id}/status")
    class UpdateStatusTest {

        @Test
        @DisplayName("ステータスを更新できること")
        void shouldUpdateStatus() throws Exception {
            when(orderService.updateStatus(eq(1L), eq(OrderStatus.CONFIRMED), eq(0)))
                    .thenReturn(orderResult);

            mockMvc.perform(
                            patch("/api/admin/orders/{id}/status", 1L)
                                    .param("status", "CONFIRMED")
                                    .param("version", "0"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(orderResult.id()));
        }

        @Test
        @DisplayName("ステータスにCANCELLEDを指定した場合、注文をキャンセルすること")
        void shouldCancelWhenStatusIsCancelled() throws Exception {
            when(orderService.cancel(1L, 0)).thenReturn(orderResult);

            mockMvc.perform(
                            patch("/api/admin/orders/{id}/status", 1L)
                                    .param("status", "CANCELLED")
                                    .param("version", "0"))
                    .andExpect(status().isOk());
            verify(orderService).cancel(1L, 0);
            verify(orderService, never()).updateStatus(any(), any(), anyInt());
        }
    }
}
