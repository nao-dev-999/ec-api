package com.example.ecapi.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        // 実際の認証方式はBearer/JWTではなく、/api/auth/loginまたは
        // /api/customer/auth/login成功時にSpring Sessionが発行するセッションCookie
        // (SecurityConfig参照)。Swagger UIで動作確認する場合はログインAPIを実行すると
        // ブラウザにCookieが自動保存され、以降のリクエストで送信される。
        return new OpenAPI()
                .info(new Info().title("EC API").description("ECサイトのバックエンドAPI").version("1.0.0"))
                .addSecurityItem(new SecurityRequirement().addList("SessionCookie"))
                .components(
                        new Components()
                                .addSecuritySchemes(
                                        "SessionCookie",
                                        new SecurityScheme()
                                                .type(SecurityScheme.Type.APIKEY)
                                                .in(SecurityScheme.In.COOKIE)
                                                .name("SESSION")
                                                .description(
                                                        "/api/auth/login または"
                                                                + " /api/customer/auth/login"
                                                                + "でログイン後に発行されるセッションCookie")));
    }
}
