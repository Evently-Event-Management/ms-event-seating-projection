package com.ticketly.mseventseatingprojection.service;


import dto.projection.CategoryProjectionDTO;
import dto.projection.EventProjectionDTO;
import dto.projection.SessionProjectionDTO;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.PrematureCloseException;

import java.net.ConnectException;
import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventProjectionClient {

    private final WebClient internalApiWebClient;

    @Value("${services.event-command.base-url}")
    private String eventServiceBaseUrl;

    /**
     * Get event projection data from the command service.
     * Any connection or response errors will be wrapped in a ProjectionClientException.
     *
     * @param eventId The UUID of the event.
     * @return Mono emitting EventProjectionDTO.
     */
    public Mono<EventProjectionDTO> getEventProjectionData(UUID eventId) {
        String url = String.format("%s/internal/v1/events/%s/projection-data", eventServiceBaseUrl, eventId);
        return internalApiWebClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(EventProjectionDTO.class)
                .doOnNext(eventProjectionDTO -> log.info("Retrieved event projection data for event ID: {}", eventId))
                .timeout(Duration.ofSeconds(10))
                .onErrorMap(this::wrapError);
    }

    /**
     * Get session projection data from the command service.
     * Any connection or response errors will be wrapped in a ProjectionClientException.
     *
     * @param sessionId The UUID of the session.
     * @return Mono emitting SessionProjectionDTO.
     */
    public Mono<SessionProjectionDTO> getSessionProjectionData(UUID sessionId) {
        String url = String.format("%s/internal/v1/sessions/%s/projection-data", eventServiceBaseUrl, sessionId);
        return internalApiWebClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(SessionProjectionDTO.class)
                .doOnNext(sessionData -> log.info("Retrieved session projection data for session ID: {}", sessionId))
                .timeout(Duration.ofSeconds(10))
                .onErrorMap(this::wrapError);
    }

    /**
     * Get category projection data from the command service.
     * Any connection or response errors will be wrapped in a ProjectionClientException.
     *
     * @param categoryId The UUID of the category.
     * @return Mono emitting CategoryProjectionDTO.
     */
    public Mono<CategoryProjectionDTO> getCategoryProjectionData(UUID categoryId) {
        String url = String.format("%s/internal/v1/categories/%s/projection-data", eventServiceBaseUrl, categoryId);
        return internalApiWebClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(CategoryProjectionDTO.class)
                .doOnNext(categoryData -> log.info("Retrieved category projection data for category ID: {}", categoryId))
                .timeout(Duration.ofSeconds(10))
                .onErrorMap(this::wrapError);
    }

    /**
     * Wraps all client errors in a ProjectionClientException to make error handling consistent
     */
    private ProjectionClientException wrapError(Throwable error) {
        String message;
        ProjectionClientException.ErrorType errorType;

        if (error instanceof WebClientResponseException responseException) {
            // NEW: Check for the specific buffer limit exception first
            if (responseException.getCause() instanceof DataBufferLimitException) {
                message = "Response size exceeds buffer limit";
                errorType = ProjectionClientException.ErrorType.RESPONSE_TOO_LARGE_ERROR;
            } else {
                // Original logic for other response exceptions
                HttpStatus status = (HttpStatus) responseException.getStatusCode();
                if (status.is4xxClientError()) {
                    if (status == HttpStatus.UNAUTHORIZED) {
                        message = "Authentication failed with status 401";
                        errorType = ProjectionClientException.ErrorType.AUTHENTICATION;
                    } else if (status == HttpStatus.NOT_FOUND) {
                        message = "Resource not found with status 404";
                        errorType = ProjectionClientException.ErrorType.NOT_FOUND;
                    } else {
                        message = "Client error with status " + status.value();
                        errorType = ProjectionClientException.ErrorType.CLIENT_ERROR;
                    }
                } else {
                    message = "Server error with status " + status.value();
                    errorType = ProjectionClientException.ErrorType.SERVER_ERROR;
                }
            }
        } else if (error instanceof WebClientRequestException) {
            Throwable cause = error.getCause();
            if (cause instanceof ConnectException) {
                message = "Connection refused - command service may be down";
                errorType = ProjectionClientException.ErrorType.CONNECTION;
            } else if (cause instanceof PrematureCloseException) {
                message = "Connection prematurely closed before response";
                errorType = ProjectionClientException.ErrorType.CONNECTION;
            } else {
                message = "Request failed: " + cause.getMessage();
                errorType = ProjectionClientException.ErrorType.CONNECTION;
            }
        } else {
            message = "Unexpected error: " + error.getMessage();
            errorType = ProjectionClientException.ErrorType.UNKNOWN;
        }

        log.error("Projection client error: {} - {}", errorType, message, error);
        return new ProjectionClientException(message, error, errorType);
    }

    /**
     * Custom exception for projection client errors
     */
    @Getter
    public static class ProjectionClientException extends RuntimeException {
        private final ErrorType errorType;

        public enum ErrorType {
            AUTHENTICATION,
            CONNECTION,
            NOT_FOUND,
            CLIENT_ERROR,
            SERVER_ERROR,
            RESPONSE_TOO_LARGE_ERROR,
            UNKNOWN
        }

        public ProjectionClientException(String message, Throwable cause, ErrorType errorType) {
            super(message, cause);
            this.errorType = errorType;
        }

    }
}
