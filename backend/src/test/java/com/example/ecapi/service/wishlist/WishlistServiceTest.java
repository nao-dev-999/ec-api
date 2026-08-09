package com.example.ecapi.service.wishlist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.ecapi.entity.Product;
import com.example.ecapi.entity.WishlistItem;
import com.example.ecapi.exception.ProductNotFoundException;
import com.example.ecapi.repository.ProductRepository;
import com.example.ecapi.repository.WishlistItemRepository;
import com.example.ecapi.service.wishlist.dto.WishlistItemResult;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class WishlistServiceTest {

    @Mock private WishlistItemRepository wishlistItemRepository;
    @Mock private ProductRepository productRepository;

    @InjectMocks private WishlistService wishlistService;

    private static final Long CUSTOMER_ID = 1L;
    private static final Long PRODUCT_ID = 10L;

    private Product product;
    private WishlistItem wishlistItem;

    @BeforeEach
    void setUp() {
        product = new Product();
        product.setId(PRODUCT_ID);
        product.setName("Test Product");
        product.setPrice(BigDecimal.valueOf(500));
        product.setStock(5);

        wishlistItem = new WishlistItem();
        wishlistItem.setId(100L);
        wishlistItem.setCustomerId(CUSTOMER_ID);
        wishlistItem.setProductId(PRODUCT_ID);
        ReflectionTestUtils.setField(wishlistItem, "createdAt", Instant.now());
    }

    @Nested
    @DisplayName("getWishlist")
    class GetWishlistTest {

        @Test
        @DisplayName("顧客のお気に入り一覧を取得できること")
        void shouldReturnWishlist() {
            when(wishlistItemRepository.findAllByCustomerIdOrderByCreatedAtDesc(CUSTOMER_ID))
                    .thenReturn(List.of(wishlistItem));
            when(productRepository.findAllById(List.of(PRODUCT_ID))).thenReturn(List.of(product));

            List<WishlistItemResult> result = wishlistService.getWishlist(CUSTOMER_ID);

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().productId()).isEqualTo(PRODUCT_ID);
            assertThat(result.getFirst().productName()).isEqualTo("Test Product");
        }
    }

    @Nested
    @DisplayName("addItem")
    class AddItemTest {

        @Test
        @DisplayName("未登録の商品を追加できること")
        void shouldAddNewItem() {
            when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
            when(wishlistItemRepository.findByCustomerIdAndProductId(CUSTOMER_ID, PRODUCT_ID))
                    .thenReturn(Optional.empty());
            when(wishlistItemRepository.save(any(WishlistItem.class))).thenReturn(wishlistItem);

            WishlistItemResult result = wishlistService.addItem(CUSTOMER_ID, PRODUCT_ID);

            assertThat(result.productId()).isEqualTo(PRODUCT_ID);
            verify(wishlistItemRepository).save(any(WishlistItem.class));
        }

        @Test
        @DisplayName("すでに登録済みの場合、冪等に既存の登録を返すこと")
        void shouldReturnExistingItemWhenAlreadyAdded() {
            when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
            when(wishlistItemRepository.findByCustomerIdAndProductId(CUSTOMER_ID, PRODUCT_ID))
                    .thenReturn(Optional.of(wishlistItem));

            WishlistItemResult result = wishlistService.addItem(CUSTOMER_ID, PRODUCT_ID);

            assertThat(result.productId()).isEqualTo(PRODUCT_ID);
            verify(wishlistItemRepository, never()).save(any(WishlistItem.class));
        }

        @Test
        @DisplayName("商品が存在しない場合、ProductNotFoundException をスローすること")
        void shouldThrowExceptionWhenProductNotFound() {
            when(productRepository.findById(99L)).thenReturn(Optional.empty());

            org.assertj.core.api.Assertions.assertThatThrownBy(
                            () -> wishlistService.addItem(CUSTOMER_ID, 99L))
                    .isInstanceOf(ProductNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("removeItem")
    class RemoveItemTest {

        @Test
        @DisplayName("登録済みの商品を削除できること")
        void shouldRemoveItem() {
            wishlistService.removeItem(CUSTOMER_ID, PRODUCT_ID);
            verify(wishlistItemRepository).deleteByCustomerIdAndProductId(CUSTOMER_ID, PRODUCT_ID);
        }
    }
}
