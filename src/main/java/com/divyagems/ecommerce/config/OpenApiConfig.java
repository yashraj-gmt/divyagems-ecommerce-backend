package com.divyagems.ecommerce.config;

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

/**
 * SpringDoc OpenAPI (Swagger) configuration.
 *
 * Configures:
 * - API metadata (title, version, contact)
 * - JWT Bearer security scheme (globally applied)
 * - Server entries for dev/prod environments
 *
 * Access Swagger UI at: http://localhost:8080/api/v1/swagger-ui.html
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.servlet.context-path:/api/v1}")
    private String contextPath;

    private static final String SECURITY_SCHEME_NAME = "BearerAuth";

    @Bean
    public OpenAPI divyaGemsOpenAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080" + contextPath)
                                .description("Local Development"),
                        new Server()
                                .url("https://api.divyagems.com" + contextPath)
                                .description("Production")
                ))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Provide a valid JWT access token obtained from POST /auth/login")
                        )
                );
    }

    private Info apiInfo() {
        return new Info()
                .title("Divya Gems API")
                .description("REST API for Divya Gems — Astrology, Spiritual & Healing Products E-Commerce")
                .version("v1.0.0")
                .contact(new Contact()
                        .name("Divya Gems Support")
                        .email("support@divyagems.com")
                        .url("https://divyagems.com"))
                .license(new License()
                        .name("Proprietary")
                        .url("https://divyagems.com/terms"));
    }
}
