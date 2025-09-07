package com.reactiverates.auth.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${openapi.server.dev.url}")
    private String devUrl;

    @Value("${openapi.server.dev.description}")
    private String devDescription;

    @Value("${openapi.server.prod.url}")
    private String prodUrl;

    @Value("${openapi.server.prod.description}")
    private String prodDescription;

    @Bean
    public OpenAPI reactiveRatesOpenAPI() {
        return new OpenAPI()
                .info(createApiInfo())
                .servers(createServers())
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .components(new Components()
                        .addSecuritySchemes("Bearer Authentication", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Введите JWT токен в формате: Bearer <token>")));
    }

    private Info createApiInfo() {
        return new Info()
                .title("ReactiveRatesAuth API")
                .description("API для аутентификации и авторизации сервиса ReactiveRates")
                .version("1.0.0")
                .contact(createContact())
                .license(createLicense());
    }

    private Contact createContact() {
        return new Contact()
                .name("Dmitrii Gorbachev")
                .url("https://github.com/ddddevelopment");
    }

    private License createLicense() {
        return new License()
                .name("MIT License")
                .url("https://opensource.org/licenses/MIT");
    }

    private List<Server> createServers() {
        return List.of(
                new Server()
                        .url(devUrl)
                        .description(devDescription),
                new Server()
                        .url(prodUrl)
                        .description(prodDescription)
        );
    }
} 