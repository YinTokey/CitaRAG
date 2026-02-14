package com.yin.cita.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI citaRagOpenAPI() {
        return new OpenAPI()
                .info(new Info().title("CitaRAG API")
                        .description("API documentation for CitaRAG - RAG-based Document Analysis System")
                        .version("v1.0.0"));
    }
}
