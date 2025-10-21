package com.ticketly.mseventseatingprojection.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;

/**
 * Configuration class for Redis caching.
 * Configures cache settings including TTL and serialization.
 */
@Configuration
@EnableCaching
@RequiredArgsConstructor
@Slf4j
public class CacheConfig {

    public static final String TRENDING_EVENTS_CACHE = "trendingEvents";
    public static final String SESSION_COUNT_CACHE = "sessionCount";

    @Value("${spring.cache.redis.key-prefix:event-seating-projection-ms::}")
    private String keyPrefix;

    /**
     * Customizes the Redis cache manager with specific cache configurations.
     * Sets up different TTLs for different cache types.
     */
    @Bean
    public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer() {
        return builder -> {
            // Configure ObjectMapper for proper serialization
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());
            objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            objectMapper.activateDefaultTyping(
                    objectMapper.getPolymorphicTypeValidator(),
                    ObjectMapper.DefaultTyping.NON_FINAL
            );

            GenericJackson2JsonRedisSerializer serializer =
                    new GenericJackson2JsonRedisSerializer(objectMapper);

            // Default cache configuration with key prefix
            RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                    .entryTtl(Duration.ofHours(1))
                    .computePrefixWith(cacheName -> keyPrefix + cacheName + "::")
                    .serializeValuesWith(
                            RedisSerializationContext.SerializationPair.fromSerializer(serializer)
                    );

            // Trending events cache - 1 hour TTL
            RedisCacheConfiguration trendingConfig = defaultConfig
                    .entryTtl(Duration.ofHours(1));

            // Session count cache - 1 hour TTL
            RedisCacheConfiguration sessionCountConfig = defaultConfig
                    .entryTtl(Duration.ofHours(1));

            builder
                    .cacheDefaults(defaultConfig)
                    .withCacheConfiguration(TRENDING_EVENTS_CACHE, trendingConfig)
                    .withCacheConfiguration(SESSION_COUNT_CACHE, sessionCountConfig);

            log.info("Redis cache manager configured with key prefix: {}", keyPrefix);
        };
    }
}
