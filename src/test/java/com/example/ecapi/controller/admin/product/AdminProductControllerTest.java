package com.example.ecapi.controller.admin.product;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.ecapi.controller.customer.product.dto.CreateProductRequest;
import com.example.ecapi.controller.customer.product.dto.ProductResponse;
import com.example.ecapi.controller.customer.product.dto.UpdateProductRequest;
import com.example.ecapi.exception.GlobalExceptionHandler;
import com.example.ecapi.exception.ProductNotFoundException;
import com.example.ecapi.helper.MessageHelper;
import com.example.ecapi.service.product.ProductService;
import com.example.ecapi.service.product.dto.CreateProduct;
import com.example.ecapi.service.product.dto.ProductResult;
import com.example.ecapi.service.product.dto.UpdateProduct;
import jakarta.persistence.OptimisticLockException;
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

@WebMvcTest(AdminProductController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class})
class AdminProductControllerTest {

    @MockitoBean private ProductService productService;
    @MockitoBean private MessageHelper messageHelper;
    @Autowired private JsonMapper jsonMapper;
    @Autowired private MockMvc mockMvc;

    private ProductResult productResult;
    private ProductResponse productResponse;

    @BeforeEach
    void setUp() {
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
        productResponse =
                new ProductResponse(
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
    @DisplayName("GET /api/admin/products")
    class GetAllProductsTest {
        @Test
        @DisplayName("検索条件なしで全商品を取得できること")
        void shouldGetAllProductsWithoutCriteria() throws Exception {
            when(productService.searchProducts(null, null, null))
                    .thenReturn(List.of(productResult));

            mockMvc.perform(get("/api/admin/products"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(productResponse.id()))
                    .andExpect(jsonPath("$[0].name").value(productResponse.name()));
        }

        @Test
        @DisplayName("検索条件を指定して商品を取得できること")
        void shouldGetProductsWithCriteria() throws Exception {
            String name = "Test";
            String description = "Desc";
            BigDecimal price = BigDecimal.valueOf(150.00);

            when(productService.searchProducts(name, description, price))
                    .thenReturn(List.of(productResult));

            mockMvc.perform(
                            get("/api/admin/products")
                                    .param("name", name)
                                    .param("description", description)
                                    .param("price", price.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(productResponse.id()))
                    .andExpect(jsonPath("$[0].name").value(productResponse.name()));
        }

        @Test
        @DisplayName("検索結果が0件の場合、空のリストを返すこと")
        void shouldReturnEmptyListWhenNoProductsFound() throws Exception {
            when(productService.searchProducts(any(), any(), any()))
                    .thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/admin/products"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isEmpty());
        }
    }

    @Nested
    @DisplayName("GET /api/admin/products/{id}")
    class GetProductByIdTest {
        @Test
        @DisplayName("指定したIDの商品を取得できること")
        void shouldGetProductById() throws Exception {
            when(productService.findById(1L)).thenReturn(productResult);

            mockMvc.perform(get("/api/admin/products/{id}", 1L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(productResponse.id()))
                    .andExpect(jsonPath("$.name").value(productResponse.name()));
        }

        @Test
        @DisplayName("指定したIDの商品が見つからない場合、404を返すこと")
        void shouldReturnNotFoundWhenProductDoesNotExist() throws Exception {
            doThrow(new ProductNotFoundException("Product not found"))
                    .when(productService)
                    .findById(any(Long.class));

            mockMvc.perform(get("/api/admin/products/{id}", 99L)).andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/admin/products")
    class CreateProductTest {
        @Test
        @DisplayName("商品を新規登録できること")
        void shouldCreateProduct() throws Exception {
            CreateProductRequest request =
                    new CreateProductRequest(
                            "New Product", "New Desc", BigDecimal.valueOf(200.00), 20);

            when(productService.create(any(CreateProduct.class))).thenReturn(productResult);

            mockMvc.perform(
                            post("/api/admin/products")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(productResponse.id()))
                    .andExpect(jsonPath("$.name").value(productResponse.name()));
        }

        @Test
        @DisplayName("バリデーションエラーの場合、400を返すこと")
        void shouldReturnBadRequestWhenValidationFails() throws Exception {
            CreateProductRequest invalidRequest =
                    new CreateProductRequest("", "New Desc", BigDecimal.valueOf(-10.00), -5);

            mockMvc.perform(
                            post("/api/admin/products")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(jsonMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.details.name").exists())
                    .andExpect(jsonPath("$.details.price").exists())
                    .andExpect(jsonPath("$.details.stock").exists());
        }
    }

    @Nested
    @DisplayName("PUT /api/admin/products/{id}")
    class UpdateProductTest {
        @Test
        @DisplayName("指定したIDの商品を更新できること")
        void shouldUpdateProduct() throws Exception {
            UpdateProductRequest request =
                    new UpdateProductRequest(
                            1L,
                            "Updated Product",
                            "Updated Desc",
                            BigDecimal.valueOf(120.00),
                            15,
                            1);

            when(productService.update(any(UpdateProduct.class))).thenReturn(productResult);

            mockMvc.perform(
                            put("/api/admin/products/{id}", 1L)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(productResponse.id()))
                    .andExpect(jsonPath("$.name").value(productResponse.name()));
        }

        @Test
        @DisplayName("指定したIDの商品が見つからない場合、404を返すこと")
        void shouldReturnNotFoundWhenUpdatingNonExistentProduct() throws Exception {
            UpdateProductRequest request =
                    new UpdateProductRequest(
                            99L,
                            "Updated Product",
                            "Updated Desc",
                            BigDecimal.valueOf(120.00),
                            15,
                            1);

            doThrow(new ProductNotFoundException("Product not found"))
                    .when(productService)
                    .update(any(UpdateProduct.class));

            mockMvc.perform(
                            put("/api/admin/products/{id}", 99L)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("楽観ロックの競合が発生した場合、409を返すこと")
        void shouldReturnConflictWhenOptimisticLockExceptionOccurs() throws Exception {
            UpdateProductRequest request =
                    new UpdateProductRequest(
                            1L,
                            "Updated Product",
                            "Updated Desc",
                            BigDecimal.valueOf(120.00),
                            15,
                            1);

            doThrow(new OptimisticLockException("Optimistic lock failed"))
                    .when(productService)
                    .update(any(UpdateProduct.class));

            mockMvc.perform(
                            put("/api/admin/products/{id}", 1L)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("DELETE /api/admin/products/{id}")
    class DeleteProductTest {
        @Test
        @DisplayName("指定したIDの商品を削除できること")
        void shouldDeleteProduct() throws Exception {
            doNothing().when(productService).delete(1L);

            mockMvc.perform(delete("/api/admin/products/{id}", 1L))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("指定したIDの商品が見つからない場合、404を返すこと")
        void shouldReturnNotFoundWhenDeletingNonExistentProduct() throws Exception {
            doThrow(new ProductNotFoundException("Product not found"))
                    .when(productService)
                    .delete(any(Long.class));

            mockMvc.perform(delete("/api/admin/products/{id}", 99L))
                    .andExpect(status().isNotFound());
        }
    }
}
