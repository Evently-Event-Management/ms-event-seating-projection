package com.ticketly.mseventseatingprojection.service;


import dto.projection.CategoryProjectionDTO;
import dto.projection.EventProjectionDTO;
import dto.projection.SessionProjectionDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventProjectionClient {

    private final WebClient internalApiWebClient;

    @Value("${services.event-command.base-url}")
    private String eventServiceBaseUrl;

    public Mono<EventProjectionDTO> getEventProjectionData(UUID eventId) {
        String url = String.format("%s/internal/v1/events/%s/projection-data", eventServiceBaseUrl, eventId);
        return internalApiWebClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(EventProjectionDTO.class).doOnNext(eventProjectionDTO -> {
                    log.info(eventProjectionDTO.toString());
                });
    }

    public Mono<SessionProjectionDTO> getSessionProjectionData(UUID sessionId) {
        String url = String.format("%s/internal/v1/sessions/%s/projection-data", eventServiceBaseUrl, sessionId);
        return internalApiWebClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(SessionProjectionDTO.class);
    }

    public Mono<CategoryProjectionDTO> getCategoryProjectionData(UUID categoryId) {
        String url = String.format("%s/internal/v1/categories/%s/projection-data", eventServiceBaseUrl, categoryId);
        return internalApiWebClient.get().uri(url).retrieve().bodyToMono(CategoryProjectionDTO.class);
    }
}
