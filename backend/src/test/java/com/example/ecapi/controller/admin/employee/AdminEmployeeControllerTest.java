package com.example.ecapi.controller.admin.employee;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.ecapi.constant.EmployeeRole;
import com.example.ecapi.controller.admin.employee.dto.AdminEmployeeResponse;
import com.example.ecapi.controller.admin.employee.dto.CreateEmployeeRequest;
import com.example.ecapi.controller.admin.employee.dto.UpdateEmployeeRoleRequest;
import com.example.ecapi.exception.EmployeeEmailDuplicateException;
import com.example.ecapi.exception.EmployeeNotFoundException;
import com.example.ecapi.exception.GlobalExceptionHandler;
import com.example.ecapi.helper.MessageHelper;
import com.example.ecapi.service.employee.EmployeeService;
import com.example.ecapi.service.employee.dto.CreateEmployee;
import com.example.ecapi.service.employee.dto.EmployeeResult;
import com.example.ecapi.service.employee.dto.UpdateEmployeeRole;
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

@WebMvcTest(AdminEmployeeController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class})
class AdminEmployeeControllerTest {

    @MockitoBean private EmployeeService employeeService;
    @MockitoBean private MessageHelper messageHelper;
    @Autowired private JsonMapper jsonMapper;
    @Autowired private MockMvc mockMvc;

    private EmployeeResult employeeResult;
    private AdminEmployeeResponse employeeResponse;

    @BeforeEach
    void setUp() {
        employeeResult =
                new EmployeeResult(
                        1L,
                        "admin@example.com",
                        EmployeeRole.ADMIN,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        LocalDateTime.now(),
                        LocalDateTime.now(),
                        0);
        employeeResponse =
                new AdminEmployeeResponse(
                        1L,
                        "admin@example.com",
                        EmployeeRole.ADMIN,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        LocalDateTime.now(),
                        LocalDateTime.now(),
                        0);
    }

    @Nested
    @DisplayName("GET /api/admin/employees")
    class GetAllEmployeesTest {

        @Test
        @DisplayName("全従業員を取得できること")
        void shouldGetAllEmployees() throws Exception {
            when(employeeService.findAll()).thenReturn(List.of(employeeResult));

            mockMvc.perform(get("/api/admin/employees"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(employeeResponse.id()))
                    .andExpect(jsonPath("$[0].email").value(employeeResponse.email()))
                    .andExpect(jsonPath("$[0].role").value(employeeResponse.role().name()));
        }

        @Test
        @DisplayName("従業員が0件の場合、空のリストを返すこと")
        void shouldReturnEmptyListWhenNoEmployees() throws Exception {
            when(employeeService.findAll()).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/admin/employees"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isEmpty());
        }
    }

    @Nested
    @DisplayName("GET /api/admin/employees/{id}")
    class GetEmployeeByIdTest {

        @Test
        @DisplayName("指定したIDの従業員を取得できること")
        void shouldGetEmployeeById() throws Exception {
            when(employeeService.findById(1L)).thenReturn(employeeResult);

            mockMvc.perform(get("/api/admin/employees/{id}", 1L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(employeeResponse.id()))
                    .andExpect(jsonPath("$.email").value(employeeResponse.email()))
                    .andExpect(jsonPath("$.role").value(employeeResponse.role().name()));
        }

        @Test
        @DisplayName("指定したIDの従業員が見つからない場合、404を返すこと")
        void shouldReturnNotFoundWhenEmployeeDoesNotExist() throws Exception {
            doThrow(new EmployeeNotFoundException(99L))
                    .when(employeeService)
                    .findById(any(Long.class));

            mockMvc.perform(get("/api/admin/employees/{id}", 99L)).andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/admin/employees")
    class CreateEmployeeTest {

        @Test
        @DisplayName("従業員を新規登録できること")
        void shouldCreateEmployee() throws Exception {
            CreateEmployeeRequest request =
                    new CreateEmployeeRequest(
                            "new@example.com",
                            "password123",
                            EmployeeRole.SALES,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null);

            when(employeeService.create(any(CreateEmployee.class))).thenReturn(employeeResult);

            mockMvc.perform(
                            post("/api/admin/employees")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(employeeResponse.id()))
                    .andExpect(jsonPath("$.email").value(employeeResponse.email()));
        }

        @Test
        @DisplayName("バリデーションエラーの場合、400を返すこと")
        void shouldReturnBadRequestWhenValidationFails() throws Exception {
            String invalidRequestJson =
                    """
                    {"email":"not-an-email","password":"short","role":null}
                    """;

            mockMvc.perform(
                            post("/api/admin/employees")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(invalidRequestJson))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.details.email").exists())
                    .andExpect(jsonPath("$.details.password").exists())
                    .andExpect(jsonPath("$.details.role").exists());
        }

        @Test
        @DisplayName("メールアドレスが重複している場合、409を返すこと")
        void shouldReturnConflictWhenEmailDuplicate() throws Exception {
            CreateEmployeeRequest request =
                    new CreateEmployeeRequest(
                            "admin@example.com",
                            "password123",
                            EmployeeRole.ADMIN,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null);

            doThrow(new EmployeeEmailDuplicateException("admin@example.com"))
                    .when(employeeService)
                    .create(any(CreateEmployee.class));

            mockMvc.perform(
                            post("/api/admin/employees")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("PATCH /api/admin/employees/{id}/role")
    class UpdateEmployeeRoleTest {

        @Test
        @DisplayName("従業員のロールを更新できること")
        void shouldUpdateEmployeeRole() throws Exception {
            UpdateEmployeeRoleRequest request =
                    new UpdateEmployeeRoleRequest(EmployeeRole.SALES, 0);

            when(employeeService.updateRole(any(UpdateEmployeeRole.class)))
                    .thenReturn(employeeResult);

            mockMvc.perform(
                            patch("/api/admin/employees/{id}/role", 1L)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(employeeResponse.id()))
                    .andExpect(jsonPath("$.role").value(employeeResponse.role().name()));
        }

        @Test
        @DisplayName("指定したIDの従業員が見つからない場合、404を返すこと")
        void shouldReturnNotFoundWhenEmployeeDoesNotExist() throws Exception {
            UpdateEmployeeRoleRequest request =
                    new UpdateEmployeeRoleRequest(EmployeeRole.SALES, 0);

            doThrow(new EmployeeNotFoundException(99L))
                    .when(employeeService)
                    .updateRole(any(UpdateEmployeeRole.class));

            mockMvc.perform(
                            patch("/api/admin/employees/{id}/role", 99L)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("バリデーションエラーの場合、400を返すこと")
        void shouldReturnBadRequestWhenRoleIsBlank() throws Exception {
            String invalidRequestJson =
                    """
                    {"role":null,"version":0}
                    """;

            mockMvc.perform(
                            patch("/api/admin/employees/{id}/role", 1L)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(invalidRequestJson))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.details.role").exists());
        }
    }

    @Nested
    @DisplayName("DELETE /api/admin/employees/{id}")
    class DeleteEmployeeTest {

        @Test
        @DisplayName("指定したIDの従業員を削除できること")
        void shouldDeleteEmployee() throws Exception {
            doNothing().when(employeeService).delete(1L);

            mockMvc.perform(delete("/api/admin/employees/{id}", 1L))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("指定したIDの従業員が見つからない場合、404を返すこと")
        void shouldReturnNotFoundWhenDeletingNonExistentEmployee() throws Exception {
            doThrow(new EmployeeNotFoundException(99L))
                    .when(employeeService)
                    .delete(any(Long.class));

            mockMvc.perform(delete("/api/admin/employees/{id}", 99L))
                    .andExpect(status().isNotFound());
        }
    }
}
