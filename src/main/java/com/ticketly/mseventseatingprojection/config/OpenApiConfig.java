package com.ticketly.mseventseatingprojection.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Ticketly Event API")
                        .version("v1.0")
                        .description("Reactive Spring WebFlux API with Swagger")
                        .contact(new Contact()
                                .name("Dasun Piyumal")
                                .email("w.piyumal2319@gmail.com")));
    }
}
