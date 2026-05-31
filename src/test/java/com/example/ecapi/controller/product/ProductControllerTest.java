package com.example.ecapi.controller.product;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.ecapi.controller.product.dto.CreateProductRequest;
import com.example.ecapi.controller.product.dto.ProductResponse;
import com.example.ecapi.controller.product.dto.UpdateProductRequest;
import com.example.ecapi.controller.product.mapper.ProductApiMapper;
import com.example.ecapi.exception.GlobalExceptionHandler;
import com.example.ecapi.exception.ProductNotFoundException;
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

@WebMvcTest(ProductController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class})
class ProductControllerTest {

    @MockitoBean private ProductService productService;
    @MockitoBean private ProductApiMapper productApiMapper;
    @MockitoBean private MessageHelper messageHelper;
    @MockitoBean private JwtAuthenticationFilter jwtAuthenticationFilter;
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
    @DisplayName("GET /api/products")
    class GetAllProductsTest {
        @Test
        @DisplayName("検索条件なしで全商品を取得できること")
        void shouldGetAllProductsWithoutCriteria() throws Exception {
            when(productService.searchProducts(null, null, null))
                    .thenReturn(List.of(productResult));
            when(productApiMapper.toProductResponseList(any()))
                    .thenReturn(List.of(productResponse));

            mockMvc.perform(get("/api/products"))
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
            when(productApiMapper.toProductResponseList(any()))
                    .thenReturn(List.of(productResponse));

            mockMvc.perform(
                            get("/api/products")
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
            when(productApiMapper.toProductResponseList(any())).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/products"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isEmpty());
        }
    }

    @Nested
    @DisplayName("GET /api/products/{id}")
    class GetProductByIdTest {
        @Test
        @DisplayName("指定したIDの商品を取得できること")
        void shouldGetProductById() throws Exception {
            when(productService.findById(1L)).thenReturn(productResult);
            when(productApiMapper.toProductResponse(any())).thenReturn(productResponse);

            mockMvc.perform(get("/api/products/{id}", 1L))
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

            mockMvc.perform(get("/api/products/{id}", 99L)).andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/products")
    class CreateProductTest {
        @Test
        @DisplayName("商品を新規登録できること")
        void shouldCreateProduct() throws Exception {
            CreateProductRequest request =
                    new CreateProductRequest(
                            "New Product", "New Desc", BigDecimal.valueOf(200.00), 20);

            when(productApiMapper.toCreateProduct(any(CreateProductRequest.class)))
                    .thenReturn(
                            new CreateProduct(
                                    "New Product", "New Desc", BigDecimal.valueOf(200.00), 20));
            when(productService.create(any(CreateProduct.class))).thenReturn(productResult);
            when(productApiMapper.toProductResponse(any())).thenReturn(productResponse);

            mockMvc.perform(
                            post("/api/products")
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
                            post("/api/products")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(jsonMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.details.name").exists())
                    .andExpect(jsonPath("$.details.price").exists())
                    .andExpect(jsonPath("$.details.stock").exists());
        }
    }

    @Nested
    @DisplayName("PUT /api/products/{id}")
    class UpdateProductTest {
        @Test
        @DisplayName("指定したIDの商品を更新できること")
        void shouldUpdateProduct() throws Exception {
            UpdateProductRequest request =
                    new UpdateProductRequest(
                            "Updated Product", "Updated Desc", BigDecimal.valueOf(120.00), 15, 1);

            when(productApiMapper.toUpdateProduct(any(UpdateProductRequest.class)))
                    .thenReturn(
                            new com.example.ecapi.service.product.dto.UpdateProduct(
                                    "Updated Product",
                                    "Updated Desc",
                                    BigDecimal.valueOf(120.00),
                                    15,
                                    1));
            when(productService.update(eq(1L), any(UpdateProduct.class))).thenReturn(productResult);
            when(productApiMapper.toProductResponse(any())).thenReturn(productResponse);

            mockMvc.perform(
                            put("/api/products/{id}", 1L)
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
                            "Updated Product", "Updated Desc", BigDecimal.valueOf(120.00), 15, 1);

            when(productApiMapper.toUpdateProduct(any(UpdateProductRequest.class)))
                    .thenReturn(
                            new com.example.ecapi.service.product.dto.UpdateProduct(
                                    "Updated Product",
                                    "Updated Desc",
                                    BigDecimal.valueOf(120.00),
                                    15,
                                    1));

            doThrow(new ProductNotFoundException("Product not found"))
                    .when(productService)
                    .update(eq(99L), any(UpdateProduct.class));

            mockMvc.perform(
                            put("/api/products/{id}", 99L)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("楽観ロックの競合が発生した場合、409を返すこと")
        void shouldReturnConflictWhenOptimisticLockExceptionOccurs() throws Exception {
            UpdateProductRequest request =
                    new UpdateProductRequest(
                            "Updated Product", "Updated Desc", BigDecimal.valueOf(120.00), 15, 1);

            when(productApiMapper.toUpdateProduct(any(UpdateProductRequest.class)))
                    .thenReturn(
                            new com.example.ecapi.service.product.dto.UpdateProduct(
                                    "Updated Product",
                                    "Updated Desc",
                                    BigDecimal.valueOf(120.00),
                                    15,
                                    1));

            doThrow(new OptimisticLockException("Optimistic lock failed"))
                    .when(productService)
                    .update(eq(1L), any(UpdateProduct.class));

            mockMvc.perform(
                            put("/api/products/{id}", 1L)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("DELETE /api/products/{id}")
    class DeleteProductTest {
        @Test
        @DisplayName("指定したIDの商品を削除できること")
        void shouldDeleteProduct() throws Exception {
            doNothing().when(productService).delete(1L);

            mockMvc.perform(delete("/api/products/{id}", 1L)).andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("指定したIDの商品が見つからない場合、404を返すこと")
        void shouldReturnNotFoundWhenDeletingNonExistentProduct() throws Exception {
            doThrow(new ProductNotFoundException("Product not found"))
                    .when(productService)
                    .delete(any(Long.class));

            mockMvc.perform(delete("/api/products/{id}", 99L)).andExpect(status().isNotFound());
        }
    }
}
