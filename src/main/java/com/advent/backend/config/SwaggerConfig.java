package com.advent.backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        String jwtSchemeName = "jwtAuth"; // 스웨거에서 사용할 인증 이름

        // 1. API 요청 시 헤더에 Authorization: Bearer {token}을 넣겠다는 설정
        SecurityRequirement securityRequirement = new SecurityRequirement().addList(jwtSchemeName);

        // 2. 보안 스키마 정의
        Components components =
                new Components()
                        .addSecuritySchemes(
                                jwtSchemeName,
                                new SecurityScheme()
                                        .name(jwtSchemeName)
                                        .type(SecurityScheme.Type.HTTP) // HTTP 방식
                                        .scheme("bearer") // bearer 토큰 방식
                                        .bearerFormat("JWT")); // 포맷은 JWT

        return new OpenAPI()
                .addServersItem(new Server().url("http://localhost:8080")) // 이 줄 추가
                .addSecurityItem(securityRequirement)
                .components(components)
                .info(new Info().title("내 프로젝트 API").version("1.0.0"));
    }
}
