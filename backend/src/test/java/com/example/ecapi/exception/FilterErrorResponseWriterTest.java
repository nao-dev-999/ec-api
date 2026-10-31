package com.example.ecapi.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.example.ecapi.helper.MessageHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class FilterErrorResponseWriterTest {

    @Mock private MessageHelper messageHelper;

    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private FilterErrorResponseWriter writer;

    @BeforeEach
    void setUp() {
        writer = new FilterErrorResponseWriter(messageHelper, jsonMapper);
    }

    @Test
    @DisplayName("未認証アクセスは ErrorResponse 形式の 401 を返すこと")
    void commence_whenUnauthenticated_shouldWrite401ErrorResponse() throws Exception {
        given(messageHelper.get("error.unauthorized")).willReturn("認証が必要です");
        var response = new MockHttpServletResponse();

        writer.commence(
                new MockHttpServletRequest(),
                response,
                new InsufficientAuthenticationException("no auth"));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).startsWith("application/json");
        JsonNode body = jsonMapper.readTree(response.getContentAsString());
        assertThat(body.get("status").asInt()).isEqualTo(401);
        assertThat(body.get("error").asString()).isEqualTo("Unauthorized");
        assertThat(body.get("code").asString()).isEqualTo("UNAUTHORIZED");
        assertThat(body.get("message").asString()).isEqualTo("認証が必要です");
        assertThat(body.hasNonNull("timestamp")).isTrue();
    }

    @Test
    @DisplayName("権限不足は ErrorResponse 形式の 403 を返すこと")
    void handle_whenAccessDenied_shouldWrite403ErrorResponse() throws Exception {
        given(messageHelper.get("error.forbidden")).willReturn("アクセス権限がありません");
        var response = new MockHttpServletResponse();

        writer.handle(new MockHttpServletRequest(), response, new AccessDeniedException("denied"));

        assertThat(response.getStatus()).isEqualTo(403);
        JsonNode body = jsonMapper.readTree(response.getContentAsString());
        assertThat(body.get("status").asInt()).isEqualTo(403);
        assertThat(body.get("error").asString()).isEqualTo("Forbidden");
        assertThat(body.get("code").asString()).isEqualTo("FORBIDDEN");
        assertThat(body.get("message").asString()).isEqualTo("アクセス権限がありません");
        assertThat(body.hasNonNull("timestamp")).isTrue();
    }

    @Test
    @DisplayName("任意の ErrorCode で ErrorResponse 形式のレスポンスを書き込めること")
    void write_withTooManyRequests_shouldWrite429ErrorResponse() throws Exception {
        given(messageHelper.get("error.tooManyRequests")).willReturn("リクエスト数の上限を超えました");
        var response = new MockHttpServletResponse();

        writer.write(response, ErrorCode.TOO_MANY_REQUESTS);

        assertThat(response.getStatus()).isEqualTo(429);
        JsonNode body = jsonMapper.readTree(response.getContentAsString());
        assertThat(body.get("error").asString()).isEqualTo("Too Many Requests");
        assertThat(body.get("code").asString()).isEqualTo("TOO_MANY_REQUESTS");
        assertThat(body.get("message").asString()).isEqualTo("リクエスト数の上限を超えました");
    }
}
