package com.ticketly.mseventseatingprojection.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.autoconfigure.web.reactive.error.AbstractErrorWebExceptionHandler;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.stereotype.Component;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@Order(-2) // High precedence to override default error handling
public class GlobalExceptionHandler extends AbstractErrorWebExceptionHandler {

    public GlobalExceptionHandler(
            ErrorAttributes errorAttributes,
            WebProperties.Resources resources,
            ApplicationContext applicationContext,
            ServerCodecConfigurer configurer) {
        super(errorAttributes, resources, applicationContext);
        this.setMessageWriters(configurer.getWriters());
        this.setMessageReaders(configurer.getReaders());
    }

    @Override
    protected RouterFunction<ServerResponse> getRoutingFunction(ErrorAttributes errorAttributes) {
        return RouterFunctions.route(
                RequestPredicates.all(), this::renderErrorResponse);
    }

    private Mono<ServerResponse> renderErrorResponse(ServerRequest request) {
        Throwable error = getError(request);
        log.error("Error occurred: {}", error.getMessage(), error);

        // Extract path from request
        String path = request.uri().getPath();

        // Handle different types of exceptions
        switch (error) {
            case ResourceNotFoundException resourceNotFoundException -> {
                return buildErrorResponse(HttpStatus.NOT_FOUND, error.getMessage(), path, null);
            }
            case WebExchangeBindException ex -> {
                Map<String, String> validationErrors = new HashMap<>();
                ex.getBindingResult().getAllErrors().forEach(err -> {
                    String fieldName = ((FieldError) err).getField();
                    String errorMessage = err.getDefaultMessage();
                    validationErrors.put(fieldName, errorMessage);
                });

                return buildErrorResponse(
                        HttpStatus.BAD_REQUEST,
                        "Validation error",
                        path,
                        validationErrors
                );
            }
            case ResponseStatusException ex -> {
                return buildErrorResponse(
                        HttpStatus.valueOf(ex.getStatusCode().value()),
                        ex.getReason(),
                        path,
                        null
                );
            }
            default -> {
                // For any other exceptions
                return buildErrorResponse(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "An unexpected error occurred",
                        path,
                        null
                );
            }
        }
    }

    private Mono<ServerResponse> buildErrorResponse(
            HttpStatus status,
            String message,
            String path,
            Map<String, String> validationErrors) {

        ErrorResponse response = ErrorResponse.builder()
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(path)
                .timestamp(LocalDateTime.now())
                .validationErrors(validationErrors)
                .build();

        return ServerResponse
                .status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(response));
    }
}
