package com.example.ecapi.controller.customer.wishlist;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.ecapi.controller.customer.wishlist.dto.AddWishlistItemRequest;
import com.example.ecapi.exception.GlobalExceptionHandler;
import com.example.ecapi.exception.ProductNotFoundException;
import com.example.ecapi.helper.MessageHelper;
import com.example.ecapi.service.wishlist.WishlistService;
import com.example.ecapi.service.wishlist.dto.WishlistItemResult;
import com.example.ecapi.support.AuthenticationPrincipalTestConfig;
import com.example.ecapi.support.WithMockLoginUser;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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

@WebMvcTest(WishlistController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, AuthenticationPrincipalTestConfig.class})
class WishlistControllerTest {

    @MockitoBean private WishlistService wishlistService;
    @MockitoBean private MessageHelper messageHelper;
    @Autowired private JsonMapper jsonMapper;
    @Autowired private MockMvc mockMvc;

    private WishlistItemResult wishlistItemResult;

    @BeforeEach
    void setUp() {
        wishlistItemResult =
                new WishlistItemResult(
                        1L, 10L, "Test Product", BigDecimal.valueOf(500), 3, LocalDateTime.now());
    }

    @Nested
    @DisplayName("GET /api/customer/wishlist")
    @WithMockLoginUser
    class GetWishlistTest {

        @Test
        @DisplayName("お気に入り一覧を取得できること")
        void shouldGetWishlist() throws Exception {
            when(wishlistService.getWishlist(anyLong())).thenReturn(List.of(wishlistItemResult));

            mockMvc.perform(get("/api/customer/wishlist"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].productId").value(wishlistItemResult.productId()))
                    .andExpect(
                            jsonPath("$[0].productName").value(wishlistItemResult.productName()));
        }

        @Test
        @DisplayName("お気に入りが0件の場合、空のリストを返すこと")
        void shouldReturnEmptyListWhenWishlistIsEmpty() throws Exception {
            when(wishlistService.getWishlist(anyLong())).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/customer/wishlist"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isEmpty());
        }
    }

    @Nested
    @DisplayName("POST /api/customer/wishlist/items")
    @WithMockLoginUser
    class AddItemTest {

        @Test
        @DisplayName("商品をお気に入りに追加できること")
        void shouldAddItemToWishlist() throws Exception {
            AddWishlistItemRequest request = new AddWishlistItemRequest(10L);
            when(wishlistService.addItem(anyLong(), any(Long.class)))
                    .thenReturn(wishlistItemResult);

            mockMvc.perform(
                            post("/api/customer/wishlist/items")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.productId").value(wishlistItemResult.productId()));
        }

        @Test
        @DisplayName("バリデーションエラーの場合、400を返すこと")
        void shouldReturnBadRequestWhenValidationFails() throws Exception {
            AddWishlistItemRequest invalidRequest = new AddWishlistItemRequest(null);

            mockMvc.perform(
                            post("/api/customer/wishlist/items")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(jsonMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("存在しない商品の場合、404を返すこと")
        void shouldReturnNotFoundWhenProductDoesNotExist() throws Exception {
            AddWishlistItemRequest request = new AddWishlistItemRequest(99L);
            doThrow(new ProductNotFoundException(99L))
                    .when(wishlistService)
                    .addItem(anyLong(), any(Long.class));

            mockMvc.perform(
                            post("/api/customer/wishlist/items")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /api/customer/wishlist/items/{productId}")
    @WithMockLoginUser
    class RemoveItemTest {

        @Test
        @DisplayName("お気に入りから商品を削除できること")
        void shouldRemoveItemFromWishlist() throws Exception {
            doNothing().when(wishlistService).removeItem(anyLong(), anyLong());

            mockMvc.perform(delete("/api/customer/wishlist/items/{productId}", 10L))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("未登録の商品を指定した場合も、冪等に204を返すこと")
        void shouldReturnNoContentWhenItemNotRegistered() throws Exception {
            doNothing().when(wishlistService).removeItem(anyLong(), anyLong());

            mockMvc.perform(delete("/api/customer/wishlist/items/{productId}", 99L))
                    .andExpect(status().isNoContent());
        }
    }
}
