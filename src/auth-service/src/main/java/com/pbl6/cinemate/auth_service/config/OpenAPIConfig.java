package com.pbl6.cinemate.auth_service.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAPIConfig {
    @Bean
    public OpenAPI orderServiceAPI() {
        return new OpenAPI()
                .info(new io.swagger.v3.oas.models.info.Info()
                        .title("Order Service API")
                        .version("1.0.0")
                        .license(new License().name("Apache 2.0"))
                        .description("API for cinemate in the microservices architecture"))
                .externalDocs(new ExternalDocumentation().description("You can refer to the documentation here ")
                        .url("http://cinemate-docs.com"))
                ;
    }
}
