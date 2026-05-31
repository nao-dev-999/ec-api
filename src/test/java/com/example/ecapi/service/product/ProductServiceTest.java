package com.example.ecapi.service.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.ecapi.config.MessageHelper;
import com.example.ecapi.entity.Product;
import com.example.ecapi.exception.ProductNotFoundException;
import com.example.ecapi.repository.ProductRepository;
import com.example.ecapi.service.product.dto.CreateProduct;
import com.example.ecapi.service.product.dto.ProductResult;
import com.example.ecapi.service.product.dto.UpdateProduct;
import com.example.ecapi.service.product.mapper.ProductEntityMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private ProductEntityMapper productEntityMapper;
    @Mock private MessageHelper messageHelper;

    @InjectMocks private ProductService productService;

    private Product product;
    private ProductResult productResult;

    @BeforeEach
    void setUp() {
        product =
                Product.builder()
                        .id(1L)
                        .name("Test Product")
                        .description("Description")
                        .price(BigDecimal.valueOf(100.00))
                        .stock(10)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .version(1)
                        .build();

        productResult =
                new ProductResult(
                        1L,
                        "Test Product",
                        "Description",
                        BigDecimal.valueOf(100.00),
                        10,
                        LocalDateTime.now(),
                        LocalDateTime.now(),
                        1);
    }

    @Nested
    @DisplayName("findAll")
    class FindAllTest {

        @Test
        @DisplayName("全商品を取得できること")
        void shouldReturnAllProducts() {
            when(productRepository.findAll()).thenReturn(List.of(product));
            when(productEntityMapper.toProductResultList(any())).thenReturn(List.of(productResult));

            List<ProductResult> result = productService.findAll();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).id()).isEqualTo(1L);
            assertThat(result.get(0).name()).isEqualTo("Test Product");
        }

        @Test
        @DisplayName("商品が0件の場合、空のリストを返すこと")
        void shouldReturnEmptyListWhenNoProducts() {
            when(productRepository.findAll()).thenReturn(Collections.emptyList());
            when(productEntityMapper.toProductResultList(any()))
                    .thenReturn(Collections.emptyList());

            List<ProductResult> result = productService.findAll();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findById")
    class FindByIdTest {

        @Test
        @DisplayName("指定したIDの商品を取得できること")
        void shouldReturnProductById() {
            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
            when(productEntityMapper.toProductResult(product)).thenReturn(productResult);

            ProductResult result = productService.findById(1L);

            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.name()).isEqualTo("Test Product");
        }

        @Test
        @DisplayName("指定したIDの商品が見つからない場合、ProductNotFoundException をスローすること")
        void shouldThrowExceptionWhenProductNotFound() {
            when(productRepository.findById(99L)).thenReturn(Optional.empty());
            when(messageHelper.get(any(), any())).thenReturn("商品が見つかりません: 99");

            assertThatThrownBy(() -> productService.findById(99L))
                    .isInstanceOf(ProductNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("searchProducts")
    class SearchProductsTest {

        @Test
        @DisplayName("検索条件なしで全商品を取得できること")
        void shouldReturnAllProductsWithoutCriteria() {
            when(productRepository.findAll(any(Specification.class), any(Sort.class)))
                    .thenReturn(List.of(product));
            when(productEntityMapper.toProductResultList(any())).thenReturn(List.of(productResult));

            List<ProductResult> result = productService.searchProducts(null, null, null);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("検索条件を指定して商品を取得できること")
        void shouldReturnFilteredProducts() {
            when(productRepository.findAll(any(Specification.class), any(Sort.class)))
                    .thenReturn(List.of(product));
            when(productEntityMapper.toProductResultList(any())).thenReturn(List.of(productResult));

            List<ProductResult> result =
                    productService.searchProducts("Test", "Desc", BigDecimal.valueOf(150.00));

            assertThat(result).hasSize(1);
            assertThat(result.get(0).name()).isEqualTo("Test Product");
        }

        @Test
        @DisplayName("条件に合致する商品がない場合、空のリストを返すこと")
        void shouldReturnEmptyListWhenNoMatch() {
            when(productRepository.findAll(any(Specification.class), any(Sort.class)))
                    .thenReturn(Collections.emptyList());
            when(productEntityMapper.toProductResultList(any()))
                    .thenReturn(Collections.emptyList());

            List<ProductResult> result = productService.searchProducts("NotExist", null, null);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("create")
    class CreateTest {

        @Test
        @DisplayName("商品を新規登録できること")
        void shouldCreateProduct() {
            CreateProduct createProduct =
                    new CreateProduct("New Product", "New Desc", BigDecimal.valueOf(200.00), 20);

            when(productEntityMapper.toProduct(createProduct)).thenReturn(product);
            when(productRepository.save(product)).thenReturn(product);
            when(productEntityMapper.toProductResult(product)).thenReturn(productResult);

            ProductResult result = productService.create(createProduct);

            assertThat(result.id()).isEqualTo(1L);
            verify(productRepository).save(product);
        }
    }

    @Nested
    @DisplayName("update")
    class UpdateTest {

        @Test
        @DisplayName("商品を更新できること")
        void shouldUpdateProduct() {
            UpdateProduct updateProduct =
                    new UpdateProduct(
                            "Updated Product", "Updated Desc", BigDecimal.valueOf(120.00), 15, 1);

            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
            when(productRepository.save(product)).thenReturn(product);
            when(productEntityMapper.toProductResult(product)).thenReturn(productResult);

            ProductResult result = productService.update(1L, updateProduct);

            assertThat(result).isNotNull();
            verify(productEntityMapper).updateProductFromUpdate(updateProduct, product);
            verify(productRepository).save(product);
        }

        @Test
        @DisplayName("指定したIDの商品が見つからない場合、ProductNotFoundException をスローすること")
        void shouldThrowExceptionWhenProductNotFound() {
            UpdateProduct updateProduct =
                    new UpdateProduct(
                            "Updated Product", "Updated Desc", BigDecimal.valueOf(120.00), 15, 1);

            when(productRepository.findById(99L)).thenReturn(Optional.empty());
            when(messageHelper.get(any(), any())).thenReturn("商品が見つかりません: 99");

            assertThatThrownBy(() -> productService.update(99L, updateProduct))
                    .isInstanceOf(ProductNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("delete")
    class DeleteTest {

        @Test
        @DisplayName("商品を削除できること")
        void shouldDeleteProduct() {
            when(productRepository.existsById(1L)).thenReturn(true);

            productService.delete(1L);

            verify(productRepository).deleteById(1L);
        }

        @Test
        @DisplayName("指定したIDの商品が見つからない場合、ProductNotFoundException をスローすること")
        void shouldThrowExceptionWhenProductNotFound() {
            when(productRepository.existsById(99L)).thenReturn(false);
            when(messageHelper.get(any(), any())).thenReturn("商品が見つかりません: 99");

            assertThatThrownBy(() -> productService.delete(99L))
                    .isInstanceOf(ProductNotFoundException.class);
        }
    }
}
