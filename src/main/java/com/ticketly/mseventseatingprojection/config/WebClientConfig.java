package com.ticketly.mseventseatingprojection.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.InMemoryReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {
    @Value("${webclient.max-in-memory-size:20971520}") // Default to 20MB if not set
    private int size;

    @Bean
    public WebClient internalApiWebClient(ReactiveClientRegistrationRepository clientRegistrations) {
        // This service stores the M2M tokens in memory.
        InMemoryReactiveOAuth2AuthorizedClientService clientService =
                new InMemoryReactiveOAuth2AuthorizedClientService(clientRegistrations);

        // This is the modern manager that handles the logic of fetching and refreshing M2M tokens.
        // It's the direct replacement for the deprecated repository you found.
        AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager authorizedClientManager =
                new AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager(clientRegistrations, clientService);

        // The filter function now uses the new, non-deprecated manager.
        ServerOAuth2AuthorizedClientExchangeFilterFunction oauth2 =
                new ServerOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager);

        // Tell the filter which client registration from your application.yml to use.
        oauth2.setDefaultClientRegistrationId("keycloak-m2m-client");

        //Increased buffer size to handle larger payloads
        final ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(size))
                .build();

        return WebClient.builder()
                .filter(oauth2)
                .exchangeStrategies(strategies)
                .build();
    }
}
