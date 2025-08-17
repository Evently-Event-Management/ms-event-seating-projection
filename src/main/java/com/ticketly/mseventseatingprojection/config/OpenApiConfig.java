package com.ticketly.mseventseatingprojection.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.*;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        // Create a Scopes object instead of Map
        Scopes scopes = new Scopes();
        scopes.addString("internal-api", "Access internal API");

        return new OpenAPI()
                .info(new Info()
                        .title("Ticketly API")
                        .version("v1.0")
                        .description("Swagger UI with OAuth2 Authentication"))
                .components(new Components()
                        .addSecuritySchemes("oauth2", new SecurityScheme()
                                .type(SecurityScheme.Type.OAUTH2)
                                .description("OAuth2 authentication with Keycloak")
                                .flows(new OAuthFlows()
                                        .authorizationCode(new OAuthFlow()
                                                .authorizationUrl("http://auth.ticketly.com:8080/realms/event-ticketing/protocol/openid-connect/auth")
                                                .tokenUrl("http://auth.ticketly.com:8080/realms/event-ticketing/protocol/openid-connect/token")
                                                .scopes(scopes)
                                        )
                                        .password(new OAuthFlow()
                                                .tokenUrl("http://auth.ticketly.com:8080/realms/event-ticketing/protocol/openid-connect/token")
                                                .scopes(scopes)
                                        ))))
                .addSecurityItem(new SecurityRequirement().addList("oauth2"));
    }
}
