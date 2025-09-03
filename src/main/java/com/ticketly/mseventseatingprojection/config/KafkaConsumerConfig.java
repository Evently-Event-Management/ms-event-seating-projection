package com.ticketly.mseventseatingprojection.config;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.ticketly.mseventseatingprojection.dto.SeatStatusChangeEventDto;
import com.ticketly.mseventseatingprojection.service.EventProjectionClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.errors.RecordDeserializationException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.ListenerExecutionFailedException;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.ExponentialBackOff;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
@Slf4j
public class KafkaConsumerConfig {

    // Use correct property path matching application.yml
    @Value("${spring.kafka.consumer.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String defaultGroupId;

    // =========================================================================
    // == BEANS FOR DEBEZIUM CONSUMER (using StringDeserializer)
    // =========================================================================

    @Bean
    public ConsumerFactory<String, String> debeziumConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, defaultGroupId + "-debezium");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> debeziumListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(debeziumConsumerFactory());
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);

        // Add error handler for debezium consumers
        DefaultErrorHandler errorHandler = getDefaultErrorHandler();
        factory.setCommonErrorHandler(errorHandler);

        return factory;
    }

    // ============================================================================
    // == BEANS FOR DEFAULT CONSUMERS (using JsonDeserializer)
    // ============================================================================

    @Bean
    public ConsumerFactory<String, Object> defaultConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, defaultGroupId);

        // Configure JsonDeserializer properties through the properties map
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, SeatStatusChangeEventDto.class.getName());

        // Important: Skip deserialization errors instead of retrying infinitely
        props.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer.class.getName());
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class.getName());

        // Use ErrorHandlingDeserializer to wrap the JsonDeserializer
        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                new ErrorHandlingDeserializer<>(new JsonDeserializer<>())
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(defaultConsumerFactory());
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);

        DefaultErrorHandler errorHandler = getDefaultErrorHandler();

        // Add retry-able exceptions (business logic failures that might succeed with retry)
        errorHandler.addRetryableExceptions(EventProjectionClient.ProjectionClientException.class);

        // Add non-retry-able exceptions (permanent failures)
        errorHandler.addNotRetryableExceptions(
            IllegalArgumentException.class,
            IllegalStateException.class,
            // Add deserialization related exceptions as non-retryable
            DeserializationException.class,
            RecordDeserializationException.class,
            JsonMappingException.class,
            InvalidFormatException.class,
            UnrecognizedPropertyException.class
        );

        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }

    // Helper method for error handler creation
    private static @NotNull DefaultErrorHandler getDefaultErrorHandler() {
        ExponentialBackOff backOff = new ExponentialBackOff(5000L, 2.0);
        backOff.setMaxInterval(300000L);
        backOff.setMaxElapsedTime(1800000L);

        return new DefaultErrorHandler(
            (record, exception) -> {
                if (isDeserializationException(exception)) {
                    // For deserialization errors, log but don't try to recover - just skip the message
                    log.warn("Skipping non-deserializable message. Topic: {}, Partition: {}, Offset: {}, Exception: {}",
                        record.topic(), record.partition(), record.offset(), exception.getMessage());
                } else {
                    // For other errors, log as error as we might retry
                    log.error("Processing failed for record. Topic: {}, Partition: {}, Offset: {}, Exception: {}",
                        record.topic(), record.partition(), record.offset(), exception.getMessage());
                }
            },
            backOff
        );
    }

    /**
     * Helper method to check if the exception is related to deserialization
     */
    private static boolean isDeserializationException(Exception exception) {
        Throwable cause = exception;

        // If it's a listener execution exception, get the cause
        if (exception instanceof ListenerExecutionFailedException) {
            cause = exception.getCause();
        }

        // Check if the exception or any of its causes is a deserialization exception
        while (cause != null) {
            if (cause instanceof DeserializationException || cause instanceof RecordDeserializationException || cause instanceof JsonMappingException) {
                return true;
            }
            cause = cause.getCause();
        }

        return false;
    }
}
