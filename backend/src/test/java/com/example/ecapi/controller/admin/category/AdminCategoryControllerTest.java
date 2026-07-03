package com.example.ecapi.controller.admin.category;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.ecapi.controller.admin.category.dto.AdminCategoryResponse;
import com.example.ecapi.controller.admin.category.dto.CreateCategoryRequest;
import com.example.ecapi.controller.admin.category.dto.UpdateCategoryRequest;
import com.example.ecapi.exception.CategoryNameDuplicateException;
import com.example.ecapi.exception.CategoryNotFoundException;
import com.example.ecapi.exception.GlobalExceptionHandler;
import com.example.ecapi.helper.MessageHelper;
import com.example.ecapi.service.category.CategoryService;
import com.example.ecapi.service.category.dto.CategoryResult;
import com.example.ecapi.service.category.dto.CreateCategory;
import com.example.ecapi.service.category.dto.UpdateCategory;
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

@WebMvcTest(AdminCategoryController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class})
class AdminCategoryControllerTest {

    @MockitoBean private CategoryService categoryService;
    @MockitoBean private MessageHelper messageHelper;
    @Autowired private JsonMapper jsonMapper;
    @Autowired private MockMvc mockMvc;

    private CategoryResult categoryResult;
    private AdminCategoryResponse categoryResponse;

    @BeforeEach
    void setUp() {
        categoryResult =
                new CategoryResult(1L, "Electronics", LocalDateTime.now(), LocalDateTime.now(), 0);
        categoryResponse =
                new AdminCategoryResponse(
                        1L, "Electronics", LocalDateTime.now(), LocalDateTime.now(), 0);
    }

    @Nested
    @DisplayName("GET /api/admin/categories")
    class GetAllCategoriesTest {

        @Test
        @DisplayName("全カテゴリを取得できること")
        void shouldGetAllCategories() throws Exception {
            when(categoryService.findAll()).thenReturn(List.of(categoryResult));

            mockMvc.perform(get("/api/admin/categories"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(categoryResponse.id()))
                    .andExpect(jsonPath("$[0].name").value(categoryResponse.name()));
        }

        @Test
        @DisplayName("カテゴリが0件の場合、空のリストを返すこと")
        void shouldReturnEmptyListWhenNoCategories() throws Exception {
            when(categoryService.findAll()).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/admin/categories"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isEmpty());
        }
    }

    @Nested
    @DisplayName("GET /api/admin/categories/{id}")
    class GetCategoryByIdTest {

        @Test
        @DisplayName("指定したIDのカテゴリを取得できること")
        void shouldGetCategoryById() throws Exception {
            when(categoryService.findById(1L)).thenReturn(categoryResult);

            mockMvc.perform(get("/api/admin/categories/{id}", 1L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(categoryResponse.id()))
                    .andExpect(jsonPath("$.name").value(categoryResponse.name()));
        }

        @Test
        @DisplayName("指定したIDのカテゴリが見つからない場合、404を返すこと")
        void shouldReturnNotFoundWhenCategoryDoesNotExist() throws Exception {
            doThrow(new CategoryNotFoundException(99L))
                    .when(categoryService)
                    .findById(any(Long.class));

            mockMvc.perform(get("/api/admin/categories/{id}", 99L))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/admin/categories")
    class CreateCategoryTest {

        @Test
        @DisplayName("カテゴリを新規登録できること")
        void shouldCreateCategory() throws Exception {
            CreateCategoryRequest request = new CreateCategoryRequest("Electronics");
            when(categoryService.create(any(CreateCategory.class))).thenReturn(categoryResult);

            mockMvc.perform(
                            post("/api/admin/categories")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(categoryResponse.id()))
                    .andExpect(jsonPath("$.name").value(categoryResponse.name()));
        }

        @Test
        @DisplayName("バリデーションエラーの場合、400を返すこと")
        void shouldReturnBadRequestWhenValidationFails() throws Exception {
            CreateCategoryRequest invalidRequest = new CreateCategoryRequest("");

            mockMvc.perform(
                            post("/api/admin/categories")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(jsonMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.details.name").exists());
        }

        @Test
        @DisplayName("カテゴリ名が重複している場合、409を返すこと")
        void shouldReturnConflictWhenNameDuplicate() throws Exception {
            CreateCategoryRequest request = new CreateCategoryRequest("Electronics");
            doThrow(new CategoryNameDuplicateException("Electronics"))
                    .when(categoryService)
                    .create(any(CreateCategory.class));

            mockMvc.perform(
                            post("/api/admin/categories")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("PUT /api/admin/categories/{id}")
    class UpdateCategoryTest {

        @Test
        @DisplayName("カテゴリを更新できること")
        void shouldUpdateCategory() throws Exception {
            UpdateCategoryRequest request = new UpdateCategoryRequest("Books", 0);
            when(categoryService.update(any(UpdateCategory.class))).thenReturn(categoryResult);

            mockMvc.perform(
                            put("/api/admin/categories/{id}", 1L)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(categoryResponse.id()));
        }

        @Test
        @DisplayName("指定したIDのカテゴリが見つからない場合、404を返すこと")
        void shouldReturnNotFoundWhenCategoryDoesNotExist() throws Exception {
            UpdateCategoryRequest request = new UpdateCategoryRequest("Books", 0);
            doThrow(new CategoryNotFoundException(99L))
                    .when(categoryService)
                    .update(any(UpdateCategory.class));

            mockMvc.perform(
                            put("/api/admin/categories/{id}", 99L)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /api/admin/categories/{id}")
    class DeleteCategoryTest {

        @Test
        @DisplayName("カテゴリを削除できること")
        void shouldDeleteCategory() throws Exception {
            doNothing().when(categoryService).delete(1L);

            mockMvc.perform(delete("/api/admin/categories/{id}", 1L))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("指定したIDのカテゴリが見つからない場合、404を返すこと")
        void shouldReturnNotFoundWhenDeletingNonExistentCategory() throws Exception {
            doThrow(new CategoryNotFoundException(99L))
                    .when(categoryService)
                    .delete(any(Long.class));

            mockMvc.perform(delete("/api/admin/categories/{id}", 99L))
                    .andExpect(status().isNotFound());
        }
    }
}
