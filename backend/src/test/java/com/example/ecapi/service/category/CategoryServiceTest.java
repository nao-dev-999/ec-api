package com.example.ecapi.service.category;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.ecapi.entity.Category;
import com.example.ecapi.entity.Product;
import com.example.ecapi.exception.CategoryNameDuplicateException;
import com.example.ecapi.exception.CategoryNotFoundException;
import com.example.ecapi.exception.ProductNotFoundException;
import com.example.ecapi.repository.CategoryRepository;
import com.example.ecapi.repository.ProductRepository;
import com.example.ecapi.service.category.dto.CategoryResult;
import com.example.ecapi.service.category.dto.CreateCategory;
import com.example.ecapi.service.category.dto.UpdateCategory;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
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
class CategoryServiceTest {

    @Mock private CategoryRepository categoryRepository;
    @Mock private ProductRepository productRepository;

    @InjectMocks private CategoryService categoryService;

    private Category category;
    private Product product;

    @BeforeEach
    void setUp() {
        category = new Category();
        category.setId(1L);
        category.setName("Electronics");
        ReflectionTestUtils.setField(category, "createdAt", Instant.now());
        ReflectionTestUtils.setField(category, "updatedAt", Instant.now());

        product = new Product();
        product.setId(1L);
        product.setName("Test Product");
        product.setPrice(BigDecimal.valueOf(100.00));
        product.setStock(10);
        ReflectionTestUtils.setField(product, "createdAt", Instant.now());
        ReflectionTestUtils.setField(product, "updatedAt", Instant.now());
    }

    @Nested
    @DisplayName("findAll")
    class FindAllTest {

        @Test
        @DisplayName("全カテゴリを取得できること")
        void shouldReturnAllCategories() {
            when(categoryRepository.findAll()).thenReturn(List.of(category));

            List<CategoryResult> result = categoryService.findAll();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).id()).isEqualTo(1L);
            assertThat(result.get(0).name()).isEqualTo("Electronics");
        }

        @Test
        @DisplayName("カテゴリが0件の場合、空のリストを返すこと")
        void shouldReturnEmptyListWhenNoCategories() {
            when(categoryRepository.findAll()).thenReturn(Collections.emptyList());

            List<CategoryResult> result = categoryService.findAll();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findById")
    class FindByIdTest {

        @Test
        @DisplayName("指定したIDのカテゴリを取得できること")
        void shouldReturnCategoryById() {
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

            CategoryResult result = categoryService.findById(1L);

            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.name()).isEqualTo("Electronics");
        }

        @Test
        @DisplayName("指定したIDのカテゴリが見つからない場合、CategoryNotFoundException をスローすること")
        void shouldThrowExceptionWhenCategoryNotFound() {
            when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> categoryService.findById(99L))
                    .isInstanceOf(CategoryNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("create")
    class CreateTest {

        @Test
        @DisplayName("カテゴリを新規登録できること")
        void shouldCreateCategory() {
            when(categoryRepository.existsByName("Electronics")).thenReturn(false);
            when(categoryRepository.save(any(Category.class))).thenReturn(category);

            CategoryResult result = categoryService.create(new CreateCategory("Electronics"));

            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.name()).isEqualTo("Electronics");
            verify(categoryRepository).save(any(Category.class));
        }

        @Test
        @DisplayName("カテゴリ名が重複している場合、CategoryNameDuplicateException をスローすること")
        void shouldThrowExceptionWhenNameDuplicate() {
            when(categoryRepository.existsByName("Electronics")).thenReturn(true);

            assertThatThrownBy(() -> categoryService.create(new CreateCategory("Electronics")))
                    .isInstanceOf(CategoryNameDuplicateException.class);
        }
    }

    @Nested
    @DisplayName("update")
    class UpdateTest {

        @Test
        @DisplayName("カテゴリを更新できること")
        void shouldUpdateCategory() {
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
            when(categoryRepository.existsByNameAndIdNot("Books", 1L)).thenReturn(false);
            when(categoryRepository.save(category)).thenReturn(category);

            CategoryResult result = categoryService.update(new UpdateCategory(1L, "Books", 0));

            assertThat(result).isNotNull();
            verify(categoryRepository).save(category);
        }

        @Test
        @DisplayName("指定したIDのカテゴリが見つからない場合、CategoryNotFoundException をスローすること")
        void shouldThrowExceptionWhenCategoryNotFound() {
            when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> categoryService.update(new UpdateCategory(99L, "Books", 0)))
                    .isInstanceOf(CategoryNotFoundException.class);
        }

        @Test
        @DisplayName("更新後のカテゴリ名が重複している場合、CategoryNameDuplicateException をスローすること")
        void shouldThrowExceptionWhenNameDuplicate() {
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
            when(categoryRepository.existsByNameAndIdNot("Books", 1L)).thenReturn(true);

            assertThatThrownBy(() -> categoryService.update(new UpdateCategory(1L, "Books", 0)))
                    .isInstanceOf(CategoryNameDuplicateException.class);
        }
    }

    @Nested
    @DisplayName("delete")
    class DeleteTest {

        @Test
        @DisplayName("カテゴリを削除できること")
        void shouldDeleteCategory() {
            when(categoryRepository.existsById(1L)).thenReturn(true);

            categoryService.delete(1L);

            verify(categoryRepository).deleteById(1L);
        }

        @Test
        @DisplayName("指定したIDのカテゴリが見つからない場合、CategoryNotFoundException をスローすること")
        void shouldThrowExceptionWhenCategoryNotFound() {
            when(categoryRepository.existsById(99L)).thenReturn(false);

            assertThatThrownBy(() -> categoryService.delete(99L))
                    .isInstanceOf(CategoryNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("findByProductId")
    class FindByProductIdTest {

        @Test
        @DisplayName("商品のカテゴリを取得できること")
        void shouldReturnCategoriesForProduct() {
            product.getCategories().add(category);
            when(productRepository.findById(1L)).thenReturn(Optional.of(product));

            List<CategoryResult> result = categoryService.findByProductId(1L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).name()).isEqualTo("Electronics");
        }

        @Test
        @DisplayName("商品が見つからない場合、ProductNotFoundException をスローすること")
        void shouldThrowExceptionWhenProductNotFound() {
            when(productRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> categoryService.findByProductId(99L))
                    .isInstanceOf(ProductNotFoundException.class);
        }

        @Test
        @DisplayName("カテゴリが紐付いていない場合、空のリストを返すこと")
        void shouldReturnEmptyListWhenNoCategories() {
            product.setCategories(new HashSet<>());
            when(productRepository.findById(1L)).thenReturn(Optional.of(product));

            List<CategoryResult> result = categoryService.findByProductId(1L);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("addCategoryToProduct")
    class AddCategoryToProductTest {

        @Test
        @DisplayName("商品にカテゴリを紐付けできること")
        void shouldAddCategoryToProduct() {
            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
            when(productRepository.save(product)).thenReturn(product);

            categoryService.addCategoryToProduct(1L, 1L);

            verify(productRepository).save(product);
            assertThat(product.getCategories()).contains(category);
        }

        @Test
        @DisplayName("商品が見つからない場合、ProductNotFoundException をスローすること")
        void shouldThrowExceptionWhenProductNotFound() {
            when(productRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> categoryService.addCategoryToProduct(99L, 1L))
                    .isInstanceOf(ProductNotFoundException.class);
        }

        @Test
        @DisplayName("カテゴリが見つからない場合、CategoryNotFoundException をスローすること")
        void shouldThrowExceptionWhenCategoryNotFound() {
            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
            when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> categoryService.addCategoryToProduct(1L, 99L))
                    .isInstanceOf(CategoryNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("removeCategoryFromProduct")
    class RemoveCategoryFromProductTest {

        @Test
        @DisplayName("商品からカテゴリを紐付け解除できること")
        void shouldRemoveCategoryFromProduct() {
            product.getCategories().add(category);
            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
            when(productRepository.save(product)).thenReturn(product);

            categoryService.removeCategoryFromProduct(1L, 1L);

            verify(productRepository).save(product);
            assertThat(product.getCategories()).doesNotContain(category);
        }

        @Test
        @DisplayName("商品が見つからない場合、ProductNotFoundException をスローすること")
        void shouldThrowExceptionWhenProductNotFound() {
            when(productRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> categoryService.removeCategoryFromProduct(99L, 1L))
                    .isInstanceOf(ProductNotFoundException.class);
        }
    }
}
