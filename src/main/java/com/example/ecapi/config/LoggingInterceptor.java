package com.example.ecapi.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component; // Component をインポート
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

@Component
public class LoggingInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(
            HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if (handler instanceof HandlerMethod handlerMethod) {
            Class<?> beanType = handlerMethod.getBeanType();
            Logger log = LoggerFactory.getLogger(beanType);

            log.info(
                    "Request Start: {} {} from {}",
                    request.getMethod(),
                    request.getRequestURI(),
                    request.getRemoteAddr());
        }
        return true;
    }

    @Override
    public void postHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            ModelAndView modelAndView)
            throws Exception {
        if (handler instanceof HandlerMethod handlerMethod) {
            Class<?> beanType = handlerMethod.getBeanType();
            Logger log = LoggerFactory.getLogger(beanType);
            log.info(
                    "Request End: {} {} Status: {}",
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus());
        }
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
            throws Exception {
        if (ex != null) {
            if (handler instanceof HandlerMethod handlerMethod) {
                Class<?> beanType = handlerMethod.getBeanType();
                Logger log = LoggerFactory.getLogger(beanType);
                log.error(
                        "Request Error: {} {} Exception: {}",
                        request.getMethod(),
                        request.getRequestURI(),
                        ex.getMessage());
            }
        }
    }
}
