package com.example.ecapi.filter;

import com.example.ecapi.service.auth.LoginUserDetails;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class RequestTracingFilter extends OncePerRequestFilter {

    private static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final String MDC_REQUEST_ID = "requestId";
    private static final String MDC_USER_ID = "userId";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain)
            throws ServletException, IOException {

        String requestId =
                Optional.ofNullable(request.getHeader(REQUEST_ID_HEADER))
                        .filter(s -> !s.isBlank())
                        .orElse(UUID.randomUUID().toString());

        try {
            MDC.put(MDC_REQUEST_ID, requestId);
            response.setHeader(REQUEST_ID_HEADER, requestId);

            Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                    .filter(Authentication::isAuthenticated)
                    .filter(a -> a.getPrincipal() instanceof LoginUserDetails)
                    .map(a -> ((LoginUserDetails) a.getPrincipal()).getUserId())
                    .ifPresent(id -> MDC.put(MDC_USER_ID, String.valueOf(id)));

            chain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_REQUEST_ID);
            MDC.remove(MDC_USER_ID);
        }
    }
}
