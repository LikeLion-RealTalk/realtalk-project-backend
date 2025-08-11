package com.likelion.realtalk.global.config;

import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

  @Bean
  public OpenAPI openAPI() {
    return new OpenAPI()
        .info(new Info()
            .title("RealTalk API")
            .description("RealTalk 백엔드 API 문서")
            .version("1.0.0")
            .contact(new Contact()
                .name("RealTalk Team")
                .url("https://github.com/LikeLion-RealTalk"))
            .license(new License()
                .name("MIT License")
                .url("https://opensource.org/licenses/MIT")))
        .servers(List.of(
            new Server().url("http://localhost:8080").description("개발 서버"),
            new Server().url("https://api.realtalks.co.kr:8443").description("운영 서버")
        ))
        .components(new Components()
            // JWT Bearer 인증 (헤더)
            .addSecuritySchemes("bearer-key",
                new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .in(SecurityScheme.In.HEADER)
                    .name("Authorization"))

            // 액세스 토큰 (쿠키)
            .addSecuritySchemes("access-cookie",
                new SecurityScheme()
                    .type(SecurityScheme.Type.APIKEY)
                    .in(SecurityScheme.In.COOKIE)
                    .name("accessToken"))

            // 리프레시 토큰 (쿠키)
            .addSecuritySchemes("refresh-cookie",
                new SecurityScheme()
                    .type(SecurityScheme.Type.APIKEY)
                    .in(SecurityScheme.In.COOKIE)
                    .name("refreshToken"))
        )
        .addSecurityItem(new SecurityRequirement()
            .addList("bearer-key")
            .addList("access-cookie")
            .addList("refresh-cookie")
        );
  }
}
