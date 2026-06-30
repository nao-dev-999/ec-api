package com.example.ecapi.controller.customer.order;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.ecapi.constant.OrderStatus;
import com.example.ecapi.controller.customer.order.dto.OrderItemRequest;
import com.example.ecapi.controller.customer.order.dto.OrderItemResponse;
import com.example.ecapi.controller.customer.order.dto.OrderRequest;
import com.example.ecapi.controller.customer.order.dto.OrderResponse;
import com.example.ecapi.exception.GlobalExceptionHandler;
import com.example.ecapi.exception.OrderNotFoundException;
import com.example.ecapi.helper.MessageHelper;
import com.example.ecapi.service.order.OrderService;
import com.example.ecapi.service.order.dto.OrderResult;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

@WebMvcTest(OrderController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class})
class OrderControllerTest {

    @MockitoBean private OrderService orderService;
    @MockitoBean private MessageHelper messageHelper;
    @Autowired private JsonMapper jsonMapper;
    @Autowired private MockMvc mockMvc;

    private OrderResult orderResult;
    private OrderResponse orderResponse;

    @BeforeEach
    void setUp() {
        OrderItemResponse itemResponse =
                new OrderItemResponse(
                        1L,
                        "Test Product",
                        2,
                        BigDecimal.valueOf(100.00),
                        BigDecimal.valueOf(200.00));

        orderResult =
                new OrderResult(
                        1L,
                        "Test Customer",
                        OrderStatus.PENDING,
                        BigDecimal.valueOf(200.00),
                        List.of(),
                        LocalDateTime.now(),
                        LocalDateTime.now(),
                        1);

        orderResponse =
                new OrderResponse(
                        1L,
                        "Test Customer",
                        OrderStatus.PENDING,
                        BigDecimal.valueOf(200.00),
                        List.of(itemResponse),
                        LocalDateTime.now(),
                        LocalDateTime.now(),
                        1);
    }

    @Nested
    @DisplayName("GET /api/orders")
    class GetAllOrdersTest {

        @Test
        @DisplayName("全注文を取得できること")
        void shouldGetAllOrders() throws Exception {
            when(orderService.findAll()).thenReturn(List.of(orderResult));
            mockMvc.perform(get("/api/orders"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(orderResponse.id()))
                    .andExpect(jsonPath("$[0].customerName").value(orderResponse.customerName()))
                    .andExpect(jsonPath("$[0].status").value(orderResponse.status().name()));
        }

        @Test
        @DisplayName("注文が0件の場合、空のリストを返すこと")
        void shouldReturnEmptyListWhenNoOrdersExist() throws Exception {
            mockMvc.perform(get("/api/orders"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isEmpty());
        }
    }

    @Nested
    @DisplayName("GET /api/orders/{id}")
    class GetOrderByIdTest {

        @Test
        @DisplayName("指定したIDの注文を取得できること")
        void shouldGetOrderById() throws Exception {
            when(orderService.findById(1L)).thenReturn(orderResult);
            mockMvc.perform(get("/api/orders/{id}", 1L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(orderResponse.id()))
                    .andExpect(jsonPath("$.customerName").value(orderResponse.customerName()))
                    .andExpect(jsonPath("$.status").value(orderResponse.status().name()));
        }

        @Test
        @DisplayName("指定したIDの注文が見つからない場合、404を返すこと")
        void shouldReturnNotFoundWhenOrderDoesNotExist() throws Exception {
            when(orderService.findById(any(Long.class)))
                    .thenThrow(new OrderNotFoundException("Order not found"));

            mockMvc.perform(get("/api/orders/{id}", 99L)).andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/orders")
    class CreateOrderTest {

        @Test
        @DisplayName("注文を新規作成できること")
        void shouldCreateOrder() throws Exception {
            when(orderService.create(any())).thenReturn(orderResult);

            OrderRequest request =
                    new OrderRequest("0001", "Test Customer", List.of(new OrderItemRequest(1L, 2)));

            mockMvc.perform(
                            post("/api/orders")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(orderResponse.id()))
                    .andExpect(jsonPath("$.customerName").value(orderResponse.customerName()));
        }

        @Test
        @DisplayName("customerName が空の場合、400を返すこと")
        void shouldReturnBadRequestWhenCustomerNameIsBlank() throws Exception {
            OrderRequest invalidRequest =
                    new OrderRequest("0001", "", List.of(new OrderItemRequest(1L, 2)));

            mockMvc.perform(
                            post("/api/orders")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(jsonMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("items が空の場合、400を返すこと")
        void shouldReturnBadRequestWhenItemsIsEmpty() throws Exception {
            OrderRequest invalidRequest = new OrderRequest("0001", "Test Customer", List.of());

            mockMvc.perform(
                            post("/api/orders")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(jsonMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("quantity が0以下の場合、400を返すこと")
        void shouldReturnBadRequestWhenQuantityIsZero() throws Exception {
            OrderRequest invalidRequest =
                    new OrderRequest("0001", "Test Customer", List.of(new OrderItemRequest(1L, 0)));

            mockMvc.perform(
                            post("/api/orders")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(jsonMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PATCH /api/orders/{id}/status")
    class UpdateOrderStatusTest {

        @Test
        @DisplayName("PENDING ステータスに更新できること")
        void shouldUpdateStatusToPending() throws Exception {
            when(orderService.updateStatus(1L, OrderStatus.PENDING, 0)).thenReturn(orderResult);
            mockMvc.perform(
                            patch("/api/orders/{id}/status", 1L)
                                    .param("status", "PENDING")
                                    .param("version", "0"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("PENDING"));
        }

        @Test
        @DisplayName("CANCELLED ステータスに更新すると cancel() が呼ばれること")
        void shouldCallCancelWhenStatusIsCancelled() throws Exception {
            OrderResult cancelledResult =
                    new OrderResult(
                            1L,
                            "Test Customer",
                            OrderStatus.CANCELLED,
                            BigDecimal.valueOf(200.00),
                            List.of(),
                            LocalDateTime.now(),
                            LocalDateTime.now(),
                            1);
            when(orderService.cancel(1L, 0)).thenReturn(cancelledResult);
            mockMvc.perform(
                            patch("/api/orders/{id}/status", 1L)
                                    .param("status", "CANCELLED")
                                    .param("version", "0"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CANCELLED"));
        }

        @Test
        @DisplayName("指定したIDの注文が見つからない場合、404を返すこと")
        void shouldReturnNotFoundWhenUpdatingNonExistentOrder() throws Exception {
            when(orderService.updateStatus(eq(99L), any(OrderStatus.class), anyInt()))
                    .thenThrow(new OrderNotFoundException("Order not found"));

            mockMvc.perform(
                            patch("/api/orders/{id}/status", 99L)
                                    .param("status", "CONFIRMED")
                                    .param("version", "0"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("無効なステータス値の場合、400を返すこと")
        void shouldReturnBadRequestWhenInvalidStatus() throws Exception {
            mockMvc.perform(patch("/api/orders/{id}/status", 1L).param("status", "INVALID_STATUS"))
                    .andExpect(status().isBadRequest());
        }
    }
}
