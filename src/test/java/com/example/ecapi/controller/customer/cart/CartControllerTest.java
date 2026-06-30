package com.example.ecapi.controller.customer.cart;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.ecapi.controller.customer.cart.dto.AddCartItemRequest;
import com.example.ecapi.controller.customer.cart.dto.CartItemResponse;
import com.example.ecapi.controller.customer.cart.dto.UpdateCartItemQuantityRequest;
import com.example.ecapi.exception.CartItemNotFoundException;
import com.example.ecapi.exception.GlobalExceptionHandler;
import com.example.ecapi.exception.ProductNotFoundException;
import com.example.ecapi.helper.MessageHelper;
import com.example.ecapi.service.cart.CartService;
import com.example.ecapi.service.cart.dto.AddCartItem;
import com.example.ecapi.service.cart.dto.CartItemResult;
import com.example.ecapi.service.cart.dto.UpdateCartItemQuantity;
import com.example.ecapi.support.WithMockLoginUser;
import java.math.BigDecimal;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

@WebMvcTest(CartController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class})
class CartControllerTest {

    @MockitoBean private CartService cartService;
    @MockitoBean private MessageHelper messageHelper;
    @Autowired private JsonMapper jsonMapper;
    @Autowired private MockMvc mockMvc;

    private CartItemResult cartItemResult;
    private CartItemResponse cartItemResponse;

    @BeforeEach
    void setUp() {
        cartItemResult =
                new CartItemResult(
                        1L,
                        1L,
                        10L,
                        "Test Product",
                        BigDecimal.valueOf(500),
                        2,
                        BigDecimal.valueOf(1000),
                        0);
        cartItemResponse =
                new CartItemResponse(
                        1L,
                        10L,
                        "Test Product",
                        BigDecimal.valueOf(500),
                        2,
                        BigDecimal.valueOf(1000),
                        0);
    }

    @Nested
    @DisplayName("GET /api/customer/cart")
    @WithMockLoginUser
    class GetCartTest {

        @Test
        @DisplayName("カート内の商品一覧を取得できること")
        void shouldGetCart() throws Exception {
            when(cartService.getCart(anyLong())).thenReturn(List.of(cartItemResult));

            mockMvc.perform(get("/api/customer/cart"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].productId").value(cartItemResponse.productId()))
                    .andExpect(jsonPath("$[0].productName").value(cartItemResponse.productName()))
                    .andExpect(jsonPath("$[0].quantity").value(cartItemResponse.quantity()));
        }

        @Test
        @DisplayName("カートが空の場合、空のリストを返すこと")
        void shouldReturnEmptyListWhenCartIsEmpty() throws Exception {
            when(cartService.getCart(anyLong())).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/customer/cart"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isEmpty());
        }
    }

    @Nested
    @DisplayName("POST /api/customer/cart/items")
    @WithMockLoginUser
    class AddItemTest {

        @Test
        @DisplayName("商品をカートに追加できること")
        void shouldAddItemToCart() throws Exception {
            AddCartItemRequest request = new AddCartItemRequest(10L, 2);
            when(cartService.addItem(anyLong(), any(AddCartItem.class))).thenReturn(cartItemResult);

            mockMvc.perform(
                            post("/api/customer/cart/items")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.productId").value(cartItemResponse.productId()))
                    .andExpect(jsonPath("$.quantity").value(cartItemResponse.quantity()));
        }

        @Test
        @DisplayName("バリデーションエラーの場合、400を返すこと")
        void shouldReturnBadRequestWhenValidationFails() throws Exception {
            AddCartItemRequest invalidRequest = new AddCartItemRequest(null, 0);

            mockMvc.perform(
                            post("/api/customer/cart/items")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(jsonMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("存在しない商品の場合、404を返すこと")
        void shouldReturnNotFoundWhenProductDoesNotExist() throws Exception {
            AddCartItemRequest request = new AddCartItemRequest(99L, 1);
            doThrow(new ProductNotFoundException(99L))
                    .when(cartService)
                    .addItem(anyLong(), any(AddCartItem.class));

            mockMvc.perform(
                            post("/api/customer/cart/items")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PATCH /api/customer/cart/items/{productId}")
    @WithMockLoginUser
    class UpdateQuantityTest {

        @Test
        @DisplayName("カート内の商品数量を変更できること")
        void shouldUpdateQuantity() throws Exception {
            UpdateCartItemQuantityRequest request = new UpdateCartItemQuantityRequest(5, 0);
            when(cartService.updateQuantity(anyLong(), any(UpdateCartItemQuantity.class)))
                    .thenReturn(cartItemResult);

            mockMvc.perform(
                            patch("/api/customer/cart/items/{productId}", 10L)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.productId").value(cartItemResponse.productId()));
        }

        @Test
        @DisplayName("カートに存在しない商品の場合、404を返すこと")
        void shouldReturnNotFoundWhenItemNotFound() throws Exception {
            UpdateCartItemQuantityRequest request = new UpdateCartItemQuantityRequest(5, 0);
            doThrow(new CartItemNotFoundException(99L))
                    .when(cartService)
                    .updateQuantity(anyLong(), any(UpdateCartItemQuantity.class));

            mockMvc.perform(
                            patch("/api/customer/cart/items/{productId}", 99L)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /api/customer/cart/items/{productId}")
    @WithMockLoginUser
    class RemoveItemTest {

        @Test
        @DisplayName("カートから商品を削除できること")
        void shouldRemoveItemFromCart() throws Exception {
            doNothing().when(cartService).removeItem(anyLong(), anyLong());

            mockMvc.perform(delete("/api/customer/cart/items/{productId}", 10L))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("カートに存在しない商品の場合、404を返すこと")
        void shouldReturnNotFoundWhenItemNotFound() throws Exception {
            doThrow(new CartItemNotFoundException(99L))
                    .when(cartService)
                    .removeItem(anyLong(), anyLong());

            mockMvc.perform(delete("/api/customer/cart/items/{productId}", 99L))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /api/customer/cart")
    @WithMockLoginUser
    class ClearCartTest {

        @Test
        @DisplayName("カートをクリアできること")
        void shouldClearCart() throws Exception {
            doNothing().when(cartService).clearCart(anyLong());

            mockMvc.perform(delete("/api/customer/cart")).andExpect(status().isNoContent());
        }
    }
}
