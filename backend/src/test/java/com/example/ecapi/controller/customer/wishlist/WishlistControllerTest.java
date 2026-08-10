package com.example.ecapi.controller.customer.wishlist;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.ecapi.exception.GlobalExceptionHandler;
import com.example.ecapi.exception.ProductNotFoundException;
import com.example.ecapi.exception.WishlistItemNotFoundException;
import com.example.ecapi.helper.MessageHelper;
import com.example.ecapi.service.wishlist.WishlistService;
import com.example.ecapi.service.wishlist.dto.WishlistItemResult;
import com.example.ecapi.support.AuthenticationPrincipalTestConfig;
import com.example.ecapi.support.WithMockLoginUser;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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

    private WishlistItemResult itemResult;

    @BeforeEach
    void setUp() {
        itemResult =
                new WishlistItemResult(
                        1L, 10L, "Test Product", BigDecimal.valueOf(500), 5, LocalDateTime.now());
    }

    @Nested
    @DisplayName("GET /api/customer/wishlist")
    class GetWishlistTest {

        @Test
        @WithMockLoginUser
        @DisplayName("自分のお気に入り一覧を取得できること")
        void shouldGetWishlist() throws Exception {
            Pageable pageable = PageRequest.of(0, 20);
            when(wishlistService.getWishlist(anyLong(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(java.util.List.of(itemResult), pageable, 1));

            mockMvc.perform(get("/api/customer/wishlist"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].productId").value(10))
                    .andExpect(jsonPath("$.content[0].productName").value("Test Product"));
        }

        @Test
        @WithMockLoginUser
        @DisplayName("お気に入りが0件の場合、空のリストを返すこと")
        void shouldReturnEmptyListWhenWishlistIsEmpty() throws Exception {
            Pageable pageable = PageRequest.of(0, 20);
            when(wishlistService.getWishlist(anyLong(), any(Pageable.class)))
                    .thenReturn(Page.empty(pageable));

            mockMvc.perform(get("/api/customer/wishlist"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isEmpty());
        }
    }

    @Nested
    @DisplayName("GET /api/customer/wishlist/items/{productId}")
    class GetItemTest {

        @Test
        @WithMockLoginUser
        @DisplayName("登録済みの場合、200を返すこと")
        void shouldReturnItemWhenRegistered() throws Exception {
            when(wishlistService.getItem(anyLong(), org.mockito.ArgumentMatchers.eq(10L)))
                    .thenReturn(itemResult);

            mockMvc.perform(get("/api/customer/wishlist/items/{productId}", 10L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.productId").value(10));
        }

        @Test
        @WithMockLoginUser
        @DisplayName("未登録の場合、404を返すこと")
        void shouldReturnNotFoundWhenNotRegistered() throws Exception {
            when(wishlistService.getItem(anyLong(), org.mockito.ArgumentMatchers.eq(99L)))
                    .thenThrow(new WishlistItemNotFoundException(99L));

            mockMvc.perform(get("/api/customer/wishlist/items/{productId}", 99L))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/customer/wishlist/items")
    class AddItemTest {

        @Test
        @WithMockLoginUser
        @DisplayName("お気に入りに追加できること")
        void shouldAddItem() throws Exception {
            when(wishlistService.addItem(anyLong(), org.mockito.ArgumentMatchers.eq(10L)))
                    .thenReturn(itemResult);

            mockMvc.perform(
                            post("/api/customer/wishlist/items")
                                    .contentType(
                                            org.springframework.http.MediaType.APPLICATION_JSON)
                                    .content(
                                            jsonMapper.writeValueAsString(
                                                    java.util.Map.of("productId", 10))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.productId").value(10));
        }

        @Test
        @WithMockLoginUser
        @DisplayName("商品が存在しない場合、404を返すこと")
        void shouldReturnNotFoundWhenProductNotFound() throws Exception {
            when(wishlistService.addItem(anyLong(), org.mockito.ArgumentMatchers.eq(99L)))
                    .thenThrow(new ProductNotFoundException(99L));

            mockMvc.perform(
                            post("/api/customer/wishlist/items")
                                    .contentType(
                                            org.springframework.http.MediaType.APPLICATION_JSON)
                                    .content(
                                            jsonMapper.writeValueAsString(
                                                    java.util.Map.of("productId", 99))))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockLoginUser
        @DisplayName("productIdが未指定の場合、400を返すこと")
        void shouldReturnBadRequestWhenProductIdMissing() throws Exception {
            mockMvc.perform(
                            post("/api/customer/wishlist/items")
                                    .contentType(
                                            org.springframework.http.MediaType.APPLICATION_JSON)
                                    .content(jsonMapper.writeValueAsString(java.util.Map.of())))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("DELETE /api/customer/wishlist/items/{productId}")
    class RemoveItemTest {

        @Test
        @WithMockLoginUser
        @DisplayName("お気に入りから削除できること")
        void shouldRemoveItem() throws Exception {
            mockMvc.perform(delete("/api/customer/wishlist/items/{productId}", 10L))
                    .andExpect(status().isNoContent());
            verify(wishlistService).removeItem(anyLong(), org.mockito.ArgumentMatchers.eq(10L));
        }

        @Test
        @WithMockLoginUser
        @DisplayName("未登録の商品を指定した場合も、冪等に204を返すこと")
        void shouldReturnNoContentWhenItemNotRegistered() throws Exception {
            mockMvc.perform(delete("/api/customer/wishlist/items/{productId}", 99L))
                    .andExpect(status().isNoContent());
            verify(wishlistService).removeItem(anyLong(), org.mockito.ArgumentMatchers.eq(99L));
        }
    }
}
