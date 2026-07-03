package com.example.ecapi.service.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.ecapi.entity.Product;
import com.example.ecapi.exception.ProductNotFoundException;
import com.example.ecapi.repository.ProductRepository;
import com.example.ecapi.service.product.dto.CreateProduct;
import com.example.ecapi.service.product.dto.ProductResult;
import com.example.ecapi.service.product.dto.UpdateProduct;
import java.math.BigDecimal;
import java.time.Instant;
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
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock private ProductRepository productRepository;

    @InjectMocks private ProductService productService;

    private Product product;
    private ProductResult productResult;

    @BeforeEach
    void setUp() {
        product = new Product();
        product.setId(1L);
        product.setName("Test Product");
        product.setDescription("Description");
        product.setPrice(BigDecimal.valueOf(100.00));
        product.setStock(10);
        ReflectionTestUtils.setField(product, "createdAt", Instant.now());
        ReflectionTestUtils.setField(product, "updatedAt", Instant.now());
        product.setVersion(1);

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

            List<ProductResult> result = productService.findAll();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).id()).isEqualTo(1L);
            assertThat(result.get(0).name()).isEqualTo("Test Product");
        }

        @Test
        @DisplayName("商品が0件の場合、空のリストを返すこと")
        void shouldReturnEmptyListWhenNoProducts() {
            when(productRepository.findAll()).thenReturn(Collections.emptyList());

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

            ProductResult result = productService.findById(1L);

            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.name()).isEqualTo("Test Product");
        }

        @Test
        @DisplayName("指定したIDの商品が見つからない場合、ProductNotFoundException をスローすること")
        void shouldThrowExceptionWhenProductNotFound() {
            when(productRepository.findById(99L)).thenReturn(Optional.empty());

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

            List<ProductResult> result = productService.searchProducts(null, null, null);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("検索条件を指定して商品を取得できること")
        void shouldReturnFilteredProducts() {
            when(productRepository.findAll(any(Specification.class), any(Sort.class)))
                    .thenReturn(List.of(product));

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
            when(productRepository.save(any(Product.class))).thenReturn(product);

            CreateProduct createProduct =
                    new CreateProduct("New Product", "New Desc", BigDecimal.valueOf(200.00), 20);

            ProductResult result = productService.create(createProduct);

            assertThat(result.id()).isEqualTo(1L);
            verify(productRepository).save(any(Product.class));
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
                            1L,
                            "Updated Product",
                            "Updated Desc",
                            BigDecimal.valueOf(120.00),
                            15,
                            1);

            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
            when(productRepository.save(product)).thenReturn(product);

            ProductResult result = productService.update(updateProduct);

            assertThat(result).isNotNull();
            verify(productRepository).save(product);
        }

        @Test
        @DisplayName("指定したIDの商品が見つからない場合、ProductNotFoundException をスローすること")
        void shouldThrowExceptionWhenProductNotFound() {
            UpdateProduct updateProduct =
                    new UpdateProduct(
                            99L,
                            "Updated Product",
                            "Updated Desc",
                            BigDecimal.valueOf(120.00),
                            15,
                            1);

            when(productRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.update(updateProduct))
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

            assertThatThrownBy(() -> productService.delete(99L))
                    .isInstanceOf(ProductNotFoundException.class);
        }
    }
}
