package com.example.ecapi.controller.customer.shippingaddress;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.ecapi.controller.customer.shippingaddress.dto.CreateShippingAddressRequest;
import com.example.ecapi.controller.customer.shippingaddress.dto.UpdateShippingAddressRequest;
import com.example.ecapi.exception.GlobalExceptionHandler;
import com.example.ecapi.exception.ShippingAddressNotFoundException;
import com.example.ecapi.helper.MessageHelper;
import com.example.ecapi.service.shippingaddress.ShippingAddressService;
import com.example.ecapi.service.shippingaddress.dto.ShippingAddressResult;
import com.example.ecapi.support.AuthenticationPrincipalTestConfig;
import com.example.ecapi.support.WithMockLoginUser;
import java.time.LocalDateTime;
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

@WebMvcTest(ShippingAddressController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, AuthenticationPrincipalTestConfig.class})
class ShippingAddressControllerTest {

    @MockitoBean private ShippingAddressService shippingAddressService;
    @MockitoBean private MessageHelper messageHelper;
    @Autowired private JsonMapper jsonMapper;
    @Autowired private MockMvc mockMvc;

    private ShippingAddressResult addressResult;

    @BeforeEach
    void setUp() {
        addressResult =
                new ShippingAddressResult(
                        1L,
                        "Test Recipient",
                        "100-0001",
                        "東京都",
                        "千代田区",
                        "1-1-1",
                        null,
                        "090-1111-2222",
                        true,
                        LocalDateTime.now(),
                        LocalDateTime.now(),
                        0);
    }

    @Nested
    @DisplayName("GET /api/customer/shipping-addresses")
    class ListTest {

        @Test
        @WithMockLoginUser
        @DisplayName("自分の配送先住所一覧を取得できること")
        void shouldReturnList() throws Exception {
            when(shippingAddressService.list(anyLong())).thenReturn(List.of(addressResult));

            mockMvc.perform(get("/api/customer/shipping-addresses"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(1))
                    .andExpect(jsonPath("$[0].recipientName").value("Test Recipient"));
        }

        @Test
        @WithMockLoginUser
        @DisplayName("登録がない場合、空のリストを返すこと")
        void shouldReturnEmptyListWhenNoAddresses() throws Exception {
            when(shippingAddressService.list(anyLong())).thenReturn(List.of());

            mockMvc.perform(get("/api/customer/shipping-addresses"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isEmpty());
        }
    }

    @Nested
    @DisplayName("GET /api/customer/shipping-addresses/{id}")
    class GetTest {

        @Test
        @WithMockLoginUser
        @DisplayName("自分の配送先住所を取得できること")
        void shouldReturnAddress() throws Exception {
            when(shippingAddressService.get(eq(1L), anyLong())).thenReturn(addressResult);

            mockMvc.perform(get("/api/customer/shipping-addresses/{id}", 1L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1));
        }

        @Test
        @WithMockLoginUser
        @DisplayName("存在しない、または他顧客の住所の場合、404を返すこと")
        void shouldReturnNotFoundWhenNotFound() throws Exception {
            when(shippingAddressService.get(eq(99L), anyLong()))
                    .thenThrow(new ShippingAddressNotFoundException(99L));

            mockMvc.perform(get("/api/customer/shipping-addresses/{id}", 99L))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/customer/shipping-addresses")
    class CreateTest {

        @Test
        @WithMockLoginUser
        @DisplayName("配送先住所を登録できること")
        void shouldCreateAddress() throws Exception {
            when(shippingAddressService.create(any())).thenReturn(addressResult);

            CreateShippingAddressRequest request =
                    new CreateShippingAddressRequest(
                            "Test Recipient",
                            "100-0001",
                            "東京都",
                            "千代田区",
                            "1-1-1",
                            null,
                            "090-1111-2222",
                            false);

            mockMvc.perform(
                            post("/api/customer/shipping-addresses")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1));
        }

        @Test
        @WithMockLoginUser
        @DisplayName("recipientNameが未指定の場合、400を返すこと")
        void shouldReturnBadRequestWhenRecipientNameMissing() throws Exception {
            CreateShippingAddressRequest invalidRequest =
                    new CreateShippingAddressRequest(
                            "", "100-0001", "東京都", "千代田区", "1-1-1", null, "090-1111-2222", false);

            mockMvc.perform(
                            post("/api/customer/shipping-addresses")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(jsonMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PUT /api/customer/shipping-addresses/{id}")
    class UpdateTest {

        @Test
        @WithMockLoginUser
        @DisplayName("配送先住所を更新できること")
        void shouldUpdateAddress() throws Exception {
            when(shippingAddressService.update(any())).thenReturn(addressResult);

            UpdateShippingAddressRequest request =
                    new UpdateShippingAddressRequest(
                            "Updated Recipient", null, null, null, null, null, null, null, 0);

            mockMvc.perform(
                            put("/api/customer/shipping-addresses/{id}", 1L)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1));
        }

        @Test
        @WithMockLoginUser
        @DisplayName("存在しない、または他顧客の住所の場合、404を返すこと")
        void shouldReturnNotFoundWhenNotFound() throws Exception {
            when(shippingAddressService.update(any()))
                    .thenThrow(new ShippingAddressNotFoundException(99L));

            UpdateShippingAddressRequest request =
                    new UpdateShippingAddressRequest(
                            "Updated Recipient", null, null, null, null, null, null, null, 0);

            mockMvc.perform(
                            put("/api/customer/shipping-addresses/{id}", 99L)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /api/customer/shipping-addresses/{id}")
    class DeleteTest {

        @Test
        @WithMockLoginUser
        @DisplayName("配送先住所を削除できること")
        void shouldDeleteAddress() throws Exception {
            mockMvc.perform(delete("/api/customer/shipping-addresses/{id}", 1L))
                    .andExpect(status().isNoContent());
            verify(shippingAddressService).delete(eq(1L), anyLong());
        }

        @Test
        @WithMockLoginUser
        @DisplayName("存在しない、または他顧客の住所の場合、404を返すこと")
        void shouldReturnNotFoundWhenNotFound() throws Exception {
            org.mockito.Mockito.doThrow(new ShippingAddressNotFoundException(99L))
                    .when(shippingAddressService)
                    .delete(eq(99L), anyLong());

            mockMvc.perform(delete("/api/customer/shipping-addresses/{id}", 99L))
                    .andExpect(status().isNotFound());
        }
    }
}
