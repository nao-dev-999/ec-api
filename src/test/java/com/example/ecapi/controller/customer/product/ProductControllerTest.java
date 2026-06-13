package com.example.ecapi.controller.customer.product;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.ecapi.controller.customer.product.dto.ProductResponse;
import com.example.ecapi.controller.customer.product.mapper.ProductApiMapper;
import com.example.ecapi.exception.GlobalExceptionHandler;
import com.example.ecapi.exception.ProductNotFoundException;
import com.example.ecapi.helper.MessageHelper;
import com.example.ecapi.service.product.ProductService;
import com.example.ecapi.service.product.dto.ProductResult;
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
    @DisplayName("GET /api/customer/products/{id}")
    class GetProductByIdTest {
        @Test
        @DisplayName("指定したIDの商品を取得できること")
        void shouldGetProductById() throws Exception {
            when(productService.findById(1L)).thenReturn(productResult);
            when(productApiMapper.toProductResponse(any())).thenReturn(productResponse);

            mockMvc.perform(get("/api/customer/products/{id}", 1L))
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

            mockMvc.perform(get("/api/customer/products/{id}", 99L))
                    .andExpect(status().isNotFound());
        }
    }
}
