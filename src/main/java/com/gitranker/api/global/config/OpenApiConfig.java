package com.gitranker.api.global.config;

import com.gitranker.api.global.util.CookieUtils;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI gitRankerOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Git Ranker API")
                        .version("v1")
                        .description("""
                                Machine-readable contract for Git Ranker's public `/api/v1/**` endpoints.

                                Authentication model:
                                - Protected endpoints accept either an `Authorization: Bearer <JWT>` header or the `accessToken` cookie.
                                - `/api/v1/auth/refresh` uses the `refreshToken` cookie.
                                - Initial sign-in starts with the GitHub OAuth2 redirect flow exposed by Spring Security outside `/api/v1/**`.
                                """.stripIndent()))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Send `Authorization: Bearer <JWT>` for protected API calls."))
                        .addSecuritySchemes("accessTokenCookie", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.COOKIE)
                                .name(CookieUtils.ACCESS_TOKEN_COOKIE_NAME)
                                .description("Browser session alternative to bearerAuth."))
                        .addSecuritySchemes("refreshTokenCookie", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.COOKIE)
                                .name(CookieUtils.REFRESH_TOKEN_COOKIE_NAME)
                                .description("Required by refresh and logout flows that rotate or revoke session tokens.")));
    }
}
