//package com.ticketly.mseventseatingprojection.config;
//
//import com.ticketly.mseventseatingprojection.service.EventProjectionClient;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.kafka.clients.consumer.ConsumerConfig;
//import org.apache.kafka.common.serialization.StringDeserializer;
//import org.jetbrains.annotations.NotNull;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.kafka.annotation.EnableKafka;
//import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
//import org.springframework.kafka.core.ConsumerFactory;
//import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
//import org.springframework.kafka.listener.ContainerProperties;
//import org.springframework.kafka.listener.DefaultErrorHandler;
//import org.springframework.kafka.support.serializer.JsonDeserializer;
//import org.springframework.util.backoff.ExponentialBackOff;
//
//import java.util.HashMap;
//import java.util.Map;
//
//@Configuration
//@EnableKafka
//@Slf4j
//public class KafkaConsumerConfig {
//
//    @Value("${spring.kafka.consumer.bootstrap-servers}")
//    private String bootstrapServers;
//
//    @Value("${spring.kafka.consumer.group-id}")
//    private String defaultGroupId;
//
//    /**
//     * Configure Kafka consumer with error handling and retry logic.
//     * Messages will not be acknowledged when projection client errors occur.
//     */
//    @Bean
//    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
//            ConsumerFactory<String, String> consumerFactory) {
//
//        ConcurrentKafkaListenerContainerFactory<String, String> factory =
//                new ConcurrentKafkaListenerContainerFactory<>();
//        factory.setConsumerFactory(consumerFactory);
//
//        // Configure MANUAL acknowledgment mode to control when messages are committed
//        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
//
//        // Create an exponential backoff policy
//        DefaultErrorHandler errorHandler = getDefaultErrorHandler();
//
//        // All ProjectionClient exceptions are retryable
//        errorHandler.addRetryableExceptions(EventProjectionClient.ProjectionClientException.class);
//
//        // Don't retry parsing errors
//        errorHandler.addNotRetryableExceptions(IllegalArgumentException.class);
//        errorHandler.addNotRetryableExceptions(IllegalStateException.class);
//
//        factory.setCommonErrorHandler(errorHandler);
//        return factory;
//    }
//
//    private static @NotNull DefaultErrorHandler getDefaultErrorHandler() {
//        ExponentialBackOff backOff = new ExponentialBackOff();
//        backOff.setInitialInterval(5000);     // 5 seconds initial delay
//        backOff.setMultiplier(2.0);           // Double the delay on each attempt
//        backOff.setMaxInterval(300000);       // Max 5 minute delay between retries
//        backOff.setMaxElapsedTime(1800000);   // Give up after 30 minutes of retries
//
//        // Configure error handler with retry logic
//        return new DefaultErrorHandler((record, exception) -> log.error("Processing failed for record. Topic: {}, Partition: {}, Offset: {}, Exception: {}",
//                record.topic(), record.partition(), record.offset(), exception.getMessage()), backOff);
//    }
//
//    @Bean
//    public ConsumerFactory<String, String> debeziumConsumerFactory() {
//        Map<String, Object> props = new HashMap<>();
//        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
//        props.put(ConsumerConfig.GROUP_ID_CONFIG, "debezium-projection-group");
//        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
//
//        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
//        return new DefaultKafkaConsumerFactory<>(props);
//    }
//
//    @Bean
//    public ConcurrentKafkaListenerContainerFactory<String, String> debeziumListenerContainerFactory() {
//        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
//        // Wire it up with the specific String-based consumer factory
//        factory.setConsumerFactory(debeziumConsumerFactory());
//        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
//        return factory;
//    }
//
//    @Bean
//    public ConsumerFactory<String, Object> defaultConsumerFactory() {
//        Map<String, Object> props = new HashMap<>();
//        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
//        props.put(ConsumerConfig.GROUP_ID_CONFIG, defaultGroupId);
//        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
//        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, "com.ticketly.mseventseatingprojection.dto.SeatStatusChangeEventDto");
//        return new DefaultKafkaConsumerFactory<>(
//                props,
//                new StringDeserializer(),
//                new JsonDeserializer<>()
//        );
//    }
//
//    @Bean
//    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
//        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
//                new ConcurrentKafkaListenerContainerFactory<>();
//        factory.setConsumerFactory(defaultConsumerFactory()); // Use the explicit factory
//        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
//
//        DefaultErrorHandler errorHandler = getDefaultErrorHandler();
//        errorHandler.addRetryableExceptions(EventProjectionClient.ProjectionClientException.class);
//        errorHandler.addNotRetryableExceptions(IllegalArgumentException.class, IllegalStateException.class);
//        factory.setCommonErrorHandler(errorHandler);
//        return factory;
//    }
//}

package com.ticketly.mseventseatingprojection.config;

import com.ticketly.mseventseatingprojection.dto.SeatStatusChangeEventDto;
import com.ticketly.mseventseatingprojection.service.EventProjectionClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
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
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "debezium-projection-group");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        // Set auto.offset.reset to ensure messages are received
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

        return new DefaultKafkaConsumerFactory<>(
            props,
            new StringDeserializer(),
            new JsonDeserializer<>()
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(defaultConsumerFactory());
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);

        DefaultErrorHandler errorHandler = getDefaultErrorHandler();
        errorHandler.addRetryableExceptions(EventProjectionClient.ProjectionClientException.class);
        errorHandler.addNotRetryableExceptions(IllegalArgumentException.class, IllegalStateException.class);
        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }

    // Helper method for error handler creation
    private static @NotNull DefaultErrorHandler getDefaultErrorHandler() {
        ExponentialBackOff backOff = new ExponentialBackOff(5000L, 2.0);
        backOff.setMaxInterval(300000L);
        backOff.setMaxElapsedTime(1800000L);
        return new DefaultErrorHandler((record, exception) -> log.error("Processing failed for record. Topic: {}, Partition: {}, Offset: {}, Exception: {}",
                record.topic(), record.partition(), record.offset(), exception.getMessage()), backOff);
    }
}
