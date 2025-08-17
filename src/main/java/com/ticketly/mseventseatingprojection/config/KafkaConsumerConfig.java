package com.ticketly.mseventseatingprojection.config;

import com.ticketly.mseventseatingprojection.service.EventProjectionClient;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;

@Configuration
@EnableKafka
@Slf4j
public class KafkaConsumerConfig {

    /**
     * Configure Kafka consumer with error handling and retry logic.
     * Messages will not be acknowledged when projection client errors occur.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
            ConsumerFactory<String, String> consumerFactory) {

        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);

        // Configure MANUAL acknowledgment mode to control when messages are committed
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);

        // Create an exponential backoff policy
        DefaultErrorHandler errorHandler = getDefaultErrorHandler();

        // All ProjectionClient exceptions are retryable
        errorHandler.addRetryableExceptions(EventProjectionClient.ProjectionClientException.class);

        // Don't retry parsing errors
        errorHandler.addNotRetryableExceptions(IllegalArgumentException.class);
        errorHandler.addNotRetryableExceptions(IllegalStateException.class);

        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }

    private static @NotNull DefaultErrorHandler getDefaultErrorHandler() {
        ExponentialBackOff backOff = new ExponentialBackOff();
        backOff.setInitialInterval(5000);     // 5 seconds initial delay
        backOff.setMultiplier(2.0);           // Double the delay on each attempt
        backOff.setMaxInterval(300000);       // Max 5 minute delay between retries
        backOff.setMaxElapsedTime(1800000);   // Give up after 30 minutes of retries

        // Configure error handler with retry logic
        return new DefaultErrorHandler((record, exception) -> log.error("Processing failed for record. Topic: {}, Partition: {}, Offset: {}, Exception: {}",
                record.topic(), record.partition(), record.offset(), exception.getMessage()), backOff);
    }
}
