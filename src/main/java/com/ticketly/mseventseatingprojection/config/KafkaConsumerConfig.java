package com.ticketly.mseventseatingprojection.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.ticketly.mseventseatingprojection.dto.SeatStatusChangeEventDto;
import com.ticketly.mseventseatingprojection.exception.NonRetryableProjectionException;
import com.ticketly.mseventseatingprojection.service.EventProjectionClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
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

    @Value("${spring.kafka.consumer.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String defaultGroupId;

    /**
     * The single, centralized error handler for all Kafka consumers.
     * Configured with an exponential backoff for retries and a Dead-Letter Queue (DLQ) for permanent failures.
     * @param template The KafkaTemplate used to send messages to the DLQ.
     * @return A fully configured DefaultErrorHandler.
     */
    @Bean
    public DefaultErrorHandler errorHandler(KafkaTemplate<String, String> template) {
        // 1. After retries are exhausted, send the failed message to a DLQ.
        // The topic will be the original topic name + ".DLT" (e.g., dbz.ticketly.public.discounts.DLT)
        var recoverer = new DeadLetterPublishingRecoverer(template);

        // 2. Configure the retry policy (e.g., start at 2s, multiply by 2, max 1 minute between retries)
        var backOff = new ExponentialBackOff(2000L, 2.0);
        backOff.setMaxInterval(60000L);

        var errorHandler = new DefaultErrorHandler(recoverer, backOff);

        // 3. Classify exceptions to determine if a retry should happen.
        errorHandler.addRetryableExceptions(
                EventProjectionClient.ProjectionClientException.class // Transient network errors
        );

        errorHandler.addNotRetryableExceptions(
                NonRetryableProjectionException.class, // Our custom exception for parsing errors in business logic
                JsonProcessingException.class,         // All Jackson JSON parsing errors
                DeserializationException.class,        // General Kafka deserialization errors
                IllegalArgumentException.class         // Errors from invalid data that won't be fixed by a retry
        );

        return errorHandler;
    }

    // =========================================================================
    // == FACTORY FOR DEBEZIUM CONSUMER (String Deserializer)
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
    public ConcurrentKafkaListenerContainerFactory<String, String> debeziumListenerContainerFactory(
            ConsumerFactory<String, String> debeziumConsumerFactory,
            DefaultErrorHandler errorHandler // ✅ Inject the centralized error handler
    ) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(debeziumConsumerFactory);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        factory.setCommonErrorHandler(errorHandler); // ✅ Apply the handler
        return factory;
    }

    // ============================================================================
    // == FACTORY FOR DEFAULT CONSUMERS (JSON Deserializer)
    // ============================================================================
    @Bean
    public ConsumerFactory<String, Object> defaultConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, defaultGroupId);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, SeatStatusChangeEventDto.class.getName());
        props.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer.class.getName());
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class.getName());

        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                new ErrorHandlingDeserializer<>(new JsonDeserializer<>())
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> defaultConsumerFactory,
            DefaultErrorHandler errorHandler // ✅ Inject the centralized error handler
    ) {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(defaultConsumerFactory);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        factory.setCommonErrorHandler(errorHandler); // ✅ Apply the handler
        return factory;
    }
}