package com.example.ecapi.exception;

import com.example.ecapi.helper.MessageHelper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import tools.jackson.databind.json.JsonMapper;

/**
 * フィルター層で返すエラーレスポンスを {@link ErrorResponse} 形式で書き込む。
 *
 * <p>フィルターで返すエラーは DispatcherServlet に届かないため {@code @RestControllerAdvice} では処理できない。このクラスで {@code
 * GlobalExceptionHandler} と同じレスポンス形式にそろえる。
 *
 * <ul>
 *   <li>Spring Security の 401 / 403: {@code SecurityConfig} の {@code exceptionHandling()} に登録する
 *   <li>レート制限の 429 など独自フィルターのエラー: {@link #write(HttpServletResponse, ErrorCode)} を呼ぶ
 * </ul>
 */
@RequiredArgsConstructor
public class FilterErrorResponseWriter implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final MessageHelper messageHelper;
    private final JsonMapper jsonMapper;

    /** 未認証アクセス → 401 Unauthorized */
    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException)
            throws IOException {
        write(response, ErrorCode.UNAUTHORIZED);
    }

    /** 権限不足 → 403 Forbidden */
    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException)
            throws IOException {
        write(response, ErrorCode.FORBIDDEN);
    }

    /** {@code code} のステータスとメッセージで {@link ErrorResponse} を書き込む */
    public void write(HttpServletResponse response, ErrorCode code) throws IOException {
        var body = ErrorResponse.of(code, messageHelper.get(code.getMessageKey()), null);
        response.setStatus(code.getStatus().value());
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(jsonMapper.writeValueAsString(body));
    }
}
