package com.example.ecapi.service.cart;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.ecapi.entity.CartItem;
import com.example.ecapi.entity.Product;
import com.example.ecapi.exception.CartItemNotFoundException;
import com.example.ecapi.exception.ProductNotFoundException;
import com.example.ecapi.repository.CartItemRepository;
import com.example.ecapi.repository.ProductRepository;
import com.example.ecapi.service.cart.dto.AddCartItem;
import com.example.ecapi.service.cart.dto.CartItemResult;
import com.example.ecapi.service.cart.dto.UpdateCartItemQuantity;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
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
class CartServiceTest {

    @Mock private CartItemRepository cartItemRepository;
    @Mock private ProductRepository productRepository;

    @InjectMocks private CartService cartService;

    private CartItem cartItem;
    private Product product;
    private static final Long CUSTOMER_ID = 1L;
    private static final Long PRODUCT_ID = 10L;

    @BeforeEach
    void setUp() {
        product = new Product();
        product.setId(PRODUCT_ID);
        product.setName("Test Product");
        product.setPrice(BigDecimal.valueOf(500));
        product.setStock(20);
        ReflectionTestUtils.setField(product, "createdAt", Instant.now());
        ReflectionTestUtils.setField(product, "updatedAt", Instant.now());

        cartItem = new CartItem();
        cartItem.setId(1L);
        cartItem.setCustomerId(CUSTOMER_ID);
        cartItem.setProductId(PRODUCT_ID);
        cartItem.setQuantity(2);
        ReflectionTestUtils.setField(cartItem, "createdAt", Instant.now());
        ReflectionTestUtils.setField(cartItem, "updatedAt", Instant.now());
    }

    @Nested
    @DisplayName("getCart")
    class GetCartTest {

        @Test
        @DisplayName("カート内の商品一覧を取得できること")
        void shouldReturnCartItems() {
            when(cartItemRepository.findAllByCustomerId(CUSTOMER_ID)).thenReturn(List.of(cartItem));
            when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));

            List<CartItemResult> result = cartService.getCart(CUSTOMER_ID);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).productId()).isEqualTo(PRODUCT_ID);
            assertThat(result.get(0).quantity()).isEqualTo(2);
            assertThat(result.get(0).subtotal()).isEqualByComparingTo(BigDecimal.valueOf(1000));
        }

        @Test
        @DisplayName("カートが空の場合、空のリストを返すこと")
        void shouldReturnEmptyListWhenCartIsEmpty() {
            when(cartItemRepository.findAllByCustomerId(CUSTOMER_ID))
                    .thenReturn(Collections.emptyList());

            List<CartItemResult> result = cartService.getCart(CUSTOMER_ID);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("addItem")
    class AddItemTest {

        @Test
        @DisplayName("新規商品をカートに追加できること")
        void shouldAddNewItemToCart() {
            when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
            when(cartItemRepository.findByCustomerIdAndProductId(CUSTOMER_ID, PRODUCT_ID))
                    .thenReturn(Optional.empty());
            when(cartItemRepository.save(any(CartItem.class))).thenReturn(cartItem);

            CartItemResult result =
                    cartService.addItem(CUSTOMER_ID, new AddCartItem(PRODUCT_ID, 2));

            assertThat(result.productId()).isEqualTo(PRODUCT_ID);
            verify(cartItemRepository).save(any(CartItem.class));
        }

        @Test
        @DisplayName("既存の商品を追加した場合、数量が加算されること")
        void shouldIncrementQuantityWhenItemAlreadyExists() {
            when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
            when(cartItemRepository.findByCustomerIdAndProductId(CUSTOMER_ID, PRODUCT_ID))
                    .thenReturn(Optional.of(cartItem));
            when(cartItemRepository.save(cartItem)).thenReturn(cartItem);

            cartService.addItem(CUSTOMER_ID, new AddCartItem(PRODUCT_ID, 3));

            assertThat(cartItem.getQuantity()).isEqualTo(5);
            verify(cartItemRepository).save(cartItem);
        }

        @Test
        @DisplayName("存在しない商品を追加した場合、ProductNotFoundException をスローすること")
        void shouldThrowExceptionWhenProductNotFound() {
            when(productRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cartService.addItem(CUSTOMER_ID, new AddCartItem(99L, 1)))
                    .isInstanceOf(ProductNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("updateQuantity")
    class UpdateQuantityTest {

        @Test
        @DisplayName("カート内の商品数量を変更できること")
        void shouldUpdateItemQuantity() {
            when(cartItemRepository.findByCustomerIdAndProductId(CUSTOMER_ID, PRODUCT_ID))
                    .thenReturn(Optional.of(cartItem));
            when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
            when(cartItemRepository.save(cartItem)).thenReturn(cartItem);

            CartItemResult result =
                    cartService.updateQuantity(
                            CUSTOMER_ID, new UpdateCartItemQuantity(PRODUCT_ID, 5, 0));

            assertThat(cartItem.getQuantity()).isEqualTo(5);
            verify(cartItemRepository).save(cartItem);
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("カートに存在しない商品の場合、CartItemNotFoundException をスローすること")
        void shouldThrowExceptionWhenItemNotFound() {
            when(cartItemRepository.findByCustomerIdAndProductId(CUSTOMER_ID, 99L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(
                            () ->
                                    cartService.updateQuantity(
                                            CUSTOMER_ID, new UpdateCartItemQuantity(99L, 5, 0)))
                    .isInstanceOf(CartItemNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("removeItem")
    class RemoveItemTest {

        @Test
        @DisplayName("カートから商品を削除できること")
        void shouldRemoveItemFromCart() {
            when(cartItemRepository.findByCustomerIdAndProductId(CUSTOMER_ID, PRODUCT_ID))
                    .thenReturn(Optional.of(cartItem));

            cartService.removeItem(CUSTOMER_ID, PRODUCT_ID);

            verify(cartItemRepository).delete(cartItem);
        }

        @Test
        @DisplayName("カートに存在しない商品の場合、CartItemNotFoundException をスローすること")
        void shouldThrowExceptionWhenItemNotFound() {
            when(cartItemRepository.findByCustomerIdAndProductId(CUSTOMER_ID, 99L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> cartService.removeItem(CUSTOMER_ID, 99L))
                    .isInstanceOf(CartItemNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("clearCart")
    class ClearCartTest {

        @Test
        @DisplayName("カートを全削除できること")
        void shouldClearCart() {
            cartService.clearCart(CUSTOMER_ID);

            verify(cartItemRepository).deleteByCustomerId(CUSTOMER_ID);
        }
    }
}
