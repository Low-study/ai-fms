package com.aifms.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI (Swagger) configuration for WebFlux.
 * Access UI at: http://localhost:8080/swagger-ui.html
 * Access JSON at: http://localhost:8080/api-docs
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("AI-FMS API")
                        .description("AI Finding Management System — REST API Documentation")
                        .version("0.0.1")
                        .contact(new Contact()
                                .name("Development Team")));
    }
}
