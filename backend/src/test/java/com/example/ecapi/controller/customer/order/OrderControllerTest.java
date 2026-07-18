package com.example.ecapi.controller.customer.order;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
import com.example.ecapi.support.AuthenticationPrincipalTestConfig;
import com.example.ecapi.support.WithMockLoginUser;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

@WebMvcTest(OrderController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, AuthenticationPrincipalTestConfig.class})
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
        @WithMockLoginUser
        @DisplayName("ログイン中の顧客自身の注文を取得できること")
        void shouldGetAllOrders() throws Exception {
            Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "orderedAt"));
            when(orderService.findAllByCustomerId(anyLong(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(orderResult), pageable, 1));
            mockMvc.perform(get("/api/orders"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(orderResponse.id()))
                    .andExpect(
                            jsonPath("$.content[0].customerName")
                                    .value(orderResponse.customerName()))
                    .andExpect(
                            jsonPath("$.content[0].status").value(orderResponse.status().name()));
        }

        @Test
        @WithMockLoginUser
        @DisplayName("注文が0件の場合、空のリストを返すこと")
        void shouldReturnEmptyListWhenNoOrdersExist() throws Exception {
            when(orderService.findAllByCustomerId(anyLong(), any(Pageable.class)))
                    .thenReturn(Page.empty());
            mockMvc.perform(get("/api/orders"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isEmpty());
        }
    }

    @Nested
    @DisplayName("GET /api/orders/{id}")
    class GetOrderByIdTest {

        @Test
        @WithMockLoginUser
        @DisplayName("指定したIDの注文を取得できること")
        void shouldGetOrderById() throws Exception {
            when(orderService.findByIdForCustomer(eq(1L), anyLong())).thenReturn(orderResult);
            mockMvc.perform(get("/api/orders/{id}", 1L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(orderResponse.id()))
                    .andExpect(jsonPath("$.customerName").value(orderResponse.customerName()))
                    .andExpect(jsonPath("$.status").value(orderResponse.status().name()));
        }

        @Test
        @WithMockLoginUser
        @DisplayName("指定したIDの注文が見つからない、または他顧客の注文の場合、404を返すこと")
        void shouldReturnNotFoundWhenOrderDoesNotExist() throws Exception {
            when(orderService.findByIdForCustomer(anyLong(), anyLong()))
                    .thenThrow(new OrderNotFoundException("Order not found"));

            mockMvc.perform(get("/api/orders/{id}", 99L)).andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/orders")
    class CreateOrderTest {

        @Test
        @WithMockLoginUser
        @DisplayName("注文を新規作成できること")
        void shouldCreateOrder() throws Exception {
            when(orderService.create(any())).thenReturn(orderResult);

            OrderRequest request = new OrderRequest(List.of(new OrderItemRequest(1L, 2)));

            mockMvc.perform(
                            post("/api/orders")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(orderResponse.id()))
                    .andExpect(jsonPath("$.customerName").value(orderResponse.customerName()));
        }

        @Test
        @WithMockLoginUser
        @DisplayName("items が空の場合、400を返すこと")
        void shouldReturnBadRequestWhenItemsIsEmpty() throws Exception {
            OrderRequest invalidRequest = new OrderRequest(List.of());

            mockMvc.perform(
                            post("/api/orders")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(jsonMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockLoginUser
        @DisplayName("quantity が0以下の場合、400を返すこと")
        void shouldReturnBadRequestWhenQuantityIsZero() throws Exception {
            OrderRequest invalidRequest = new OrderRequest(List.of(new OrderItemRequest(1L, 0)));

            mockMvc.perform(
                            post("/api/orders")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(jsonMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/orders/{id}/cancel")
    class CancelOrderTest {

        @Test
        @WithMockLoginUser
        @DisplayName("注文をキャンセルできること")
        void shouldCancelOrder() throws Exception {
            OrderResult cancelledResult =
                    new OrderResult(
                            1L,
                            1L,
                            "Test Customer",
                            OrderStatus.CANCELLED,
                            BigDecimal.valueOf(200.00),
                            List.of(),
                            LocalDateTime.now(),
                            LocalDateTime.now(),
                            1);
            when(orderService.findByIdForCustomer(eq(1L), anyLong())).thenReturn(orderResult);
            when(orderService.cancel(1L, 0)).thenReturn(cancelledResult);
            mockMvc.perform(post("/api/orders/{id}/cancel", 1L).param("version", "0"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CANCELLED"));
        }

        @Test
        @WithMockLoginUser
        @DisplayName("指定したIDの注文が見つからない、または他顧客の注文の場合、404を返すこと")
        void shouldReturnNotFoundWhenCancellingNonExistentOrder() throws Exception {
            when(orderService.findByIdForCustomer(anyLong(), anyLong()))
                    .thenThrow(new OrderNotFoundException("Order not found"));

            mockMvc.perform(post("/api/orders/{id}/cancel", 99L).param("version", "0"))
                    .andExpect(status().isNotFound());
            verify(orderService, never()).cancel(anyLong(), anyInt());
        }
    }
}
