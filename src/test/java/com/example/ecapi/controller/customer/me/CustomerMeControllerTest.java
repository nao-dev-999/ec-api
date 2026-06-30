package com.example.ecapi.controller.customer.me;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.ecapi.controller.customer.me.dto.UpdateEmailRequest;
import com.example.ecapi.controller.customer.me.dto.UpdatePasswordRequest;
import com.example.ecapi.exception.CustomerEmailDuplicateException;
import com.example.ecapi.exception.GlobalExceptionHandler;
import com.example.ecapi.exception.InvalidCurrentPasswordException;
import com.example.ecapi.helper.MessageHelper;
import com.example.ecapi.service.customer.CustomerMeService;
import com.example.ecapi.service.customer.dto.CustomerResult;
import com.example.ecapi.service.customer.dto.UpdateCustomerEmail;
import com.example.ecapi.service.customer.dto.UpdateCustomerPassword;
import com.example.ecapi.support.WithMockLoginUser;
import java.time.LocalDateTime;
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

@WebMvcTest(CustomerMeController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class})
@WithMockLoginUser
class CustomerMeControllerTest {

    @MockitoBean private CustomerMeService customerMeService;
    @MockitoBean private MessageHelper messageHelper;
    @Autowired private JsonMapper jsonMapper;
    @Autowired private MockMvc mockMvc;

    private CustomerResult customerResult;

    @BeforeEach
    void setUp() {
        customerResult =
                new CustomerResult(
                        1L, "test@example.com", LocalDateTime.now(), LocalDateTime.now(), 0);
    }

    @Nested
    @DisplayName("GET /api/customer/me")
    class GetMeTest {

        @Test
        @DisplayName("自分の情報を取得できること")
        void shouldGetMyInfo() throws Exception {
            when(customerMeService.findMe(anyLong())).thenReturn(customerResult);

            mockMvc.perform(get("/api/customer/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1L))
                    .andExpect(jsonPath("$.email").value("test@example.com"));
        }
    }

    @Nested
    @DisplayName("PATCH /api/customer/me/email")
    class UpdateEmailTest {

        @Test
        @DisplayName("メールアドレスを変更できること")
        void shouldUpdateEmail() throws Exception {
            UpdateEmailRequest request = new UpdateEmailRequest("new@example.com", 0);
            CustomerResult updated =
                    new CustomerResult(
                            1L, "new@example.com", LocalDateTime.now(), LocalDateTime.now(), 0);
            when(customerMeService.updateEmail(anyLong(), any(UpdateCustomerEmail.class)))
                    .thenReturn(updated);

            mockMvc.perform(
                            patch("/api/customer/me/email")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("new@example.com"));
        }

        @Test
        @DisplayName("バリデーションエラーの場合、400を返すこと")
        void shouldReturnBadRequestWhenValidationFails() throws Exception {
            UpdateEmailRequest invalidRequest = new UpdateEmailRequest("not-an-email", 0);

            mockMvc.perform(
                            patch("/api/customer/me/email")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(jsonMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("メールアドレスが重複している場合、409を返すこと")
        void shouldReturnConflictWhenEmailDuplicate() throws Exception {
            UpdateEmailRequest request = new UpdateEmailRequest("duplicate@example.com", 0);
            doThrow(new CustomerEmailDuplicateException("duplicate@example.com"))
                    .when(customerMeService)
                    .updateEmail(anyLong(), any(UpdateCustomerEmail.class));

            mockMvc.perform(
                            patch("/api/customer/me/email")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("PATCH /api/customer/me/password")
    class UpdatePasswordTest {

        @Test
        @DisplayName("パスワードを変更できること")
        void shouldUpdatePassword() throws Exception {
            UpdatePasswordRequest request =
                    new UpdatePasswordRequest("current123", "newPassword123", 0);
            doNothing()
                    .when(customerMeService)
                    .updatePassword(anyLong(), any(UpdateCustomerPassword.class));

            mockMvc.perform(
                            patch("/api/customer/me/password")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("バリデーションエラーの場合、400を返すこと")
        void shouldReturnBadRequestWhenValidationFails() throws Exception {
            UpdatePasswordRequest invalidRequest = new UpdatePasswordRequest("", "short", 0);

            mockMvc.perform(
                            patch("/api/customer/me/password")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(jsonMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.details.currentPassword").exists())
                    .andExpect(jsonPath("$.details.newPassword").exists());
        }

        @Test
        @DisplayName("現在のパスワードが正しくない場合、400を返すこと")
        void shouldReturnBadRequestWhenCurrentPasswordInvalid() throws Exception {
            UpdatePasswordRequest request =
                    new UpdatePasswordRequest("wrongPassword", "newPassword123", 0);
            doThrow(new InvalidCurrentPasswordException())
                    .when(customerMeService)
                    .updatePassword(anyLong(), any(UpdateCustomerPassword.class));

            mockMvc.perform(
                            patch("/api/customer/me/password")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(jsonMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }
}
