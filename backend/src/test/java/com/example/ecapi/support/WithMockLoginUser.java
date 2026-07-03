package com.example.ecapi.support;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.security.test.context.support.WithSecurityContext;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@WithSecurityContext(factory = WithMockLoginUserSecurityContextFactory.class)
public @interface WithMockLoginUser {
    long userId() default 1L;

    String email() default "test@example.com";

    String role() default "ROLE_CUSTOMER";
}
