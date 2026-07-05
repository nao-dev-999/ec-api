package com.example.ecapi.controller.admin.customer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.ecapi.controller.admin.customer.dto.AdminCustomerResponse;
import com.example.ecapi.exception.CustomerNotFoundException;
import com.example.ecapi.exception.GlobalExceptionHandler;
import com.example.ecapi.helper.MessageHelper;
import com.example.ecapi.service.customer.CustomerService;
import com.example.ecapi.service.customer.dto.CustomerResult;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminCustomerController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class})
class AdminCustomerControllerTest {

    @MockitoBean private CustomerService customerService;
    @MockitoBean private MessageHelper messageHelper;
    @Autowired private MockMvc mockMvc;

    private CustomerResult customerResult;
    private AdminCustomerResponse customerResponse;

    @BeforeEach
    void setUp() {
        customerResult =
                new CustomerResult(
                        1L,
                        "test@example.com",
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
        customerResponse =
                new AdminCustomerResponse(
                        1L,
                        "test@example.com",
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
    @DisplayName("GET /api/admin/customers")
    class GetAllCustomersTest {

        @Test
        @DisplayName("全顧客を取得できること")
        void shouldGetAllCustomers() throws Exception {
            when(customerService.findAll()).thenReturn(List.of(customerResult));

            mockMvc.perform(get("/api/admin/customers"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(customerResponse.id()))
                    .andExpect(jsonPath("$[0].email").value(customerResponse.email()));
        }

        @Test
        @DisplayName("顧客が0件の場合、空のリストを返すこと")
        void shouldReturnEmptyListWhenNoCustomers() throws Exception {
            when(customerService.findAll()).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/admin/customers"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isEmpty());
        }
    }

    @Nested
    @DisplayName("GET /api/admin/customers/{id}")
    class GetCustomerByIdTest {

        @Test
        @DisplayName("指定したIDの顧客を取得できること")
        void shouldGetCustomerById() throws Exception {
            when(customerService.findById(1L)).thenReturn(customerResult);

            mockMvc.perform(get("/api/admin/customers/{id}", 1L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(customerResponse.id()))
                    .andExpect(jsonPath("$.email").value(customerResponse.email()));
        }

        @Test
        @DisplayName("指定したIDの顧客が見つからない場合、404を返すこと")
        void shouldReturnNotFoundWhenCustomerDoesNotExist() throws Exception {
            doThrow(new CustomerNotFoundException(99L))
                    .when(customerService)
                    .findById(any(Long.class));

            mockMvc.perform(get("/api/admin/customers/{id}", 99L)).andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /api/admin/customers/{id}")
    class DeleteCustomerTest {

        @Test
        @DisplayName("指定したIDの顧客を削除できること")
        void shouldDeleteCustomer() throws Exception {
            doNothing().when(customerService).delete(1L);

            mockMvc.perform(delete("/api/admin/customers/{id}", 1L))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("指定したIDの顧客が見つからない場合、404を返すこと")
        void shouldReturnNotFoundWhenDeletingNonExistentCustomer() throws Exception {
            doThrow(new CustomerNotFoundException(99L))
                    .when(customerService)
                    .delete(any(Long.class));

            mockMvc.perform(delete("/api/admin/customers/{id}", 99L))
                    .andExpect(status().isNotFound());
        }
    }
}
