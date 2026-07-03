package com.example.ecapi.config;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class ServiceLoggingAspect {

    @Pointcut("execution(* com.example.ecapi.service..*.*(..))")
    public void serviceMethods() {}

    @Around("serviceMethods()")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        Logger log = LoggerFactory.getLogger(joinPoint.getTarget().getClass());
        String method = joinPoint.getSignature().toShortString();
        long start = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            log.debug("method={} responseTimeMs={}", method, System.currentTimeMillis() - start);
            return result;
        } catch (Throwable t) {
            // 例外は GlobalExceptionHandler でログを出すため、ここでは二重にしない
            log.debug("method={} threw={}", method, t.getClass().getSimpleName());
            throw t;
        }
    }
}
