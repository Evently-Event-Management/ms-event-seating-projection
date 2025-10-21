# Redis Caching Implementation

## Overview
This document describes the Redis caching implementation for the Event Seating Projection microservice. The caching strategy improves performance by reducing database queries for frequently accessed data.

## Cached Endpoints

### 1. Top Trending Events
**Endpoint**: `GET /v1/events/trending`
- **Cache Name**: `trendingEvents`
- **Cache Key**: Based on `limit` parameter
- **TTL**: 1 hour
- **Implementation**: `EventTrendingServiceImpl.getTopTrendingEventThumbnails()`

### 2. Total Session Count
**Endpoint**: `GET /v1/events/sessions/count`
- **Cache Name**: `sessionCount`
- **Cache Key**: `'total'`
- **TTL**: 1 hour
- **Implementation**: `EventQueryService.countAllSessions()`

## Cache Configuration

### Dependencies Added (pom.xml)
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis-reactive</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>
```

### Redis Configuration (application.yml)
```yaml
spring:
  cache:
    type: redis
    redis:
      time-to-live: 3600s
      key-prefix: "event-seating-ms::"
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      timeout: ${REDIS_TIMEOUT:2000}
      repositories:
        enabled: false
```

### Cache Configuration Class
- **File**: `CacheConfig.java`
- **Features**:
  - Custom ObjectMapper with JavaTimeModule for proper date serialization
  - Separate cache configurations for trending events and session count
  - JSON serialization using GenericJackson2JsonRedisSerializer

## Cache Eviction Strategy

Cache eviction ensures that stale data is not served to clients. Caches are automatically cleared when underlying data changes.

### Trending Events Cache Eviction
The `trendingEvents` cache is evicted in the following scenarios:

#### 1. Event Changes (DebeziumEventConsumer.processEventChange)
- Event deletion
- Event approval
- Event completion
- Event status change

#### 2. Session Changes (DebeziumEventConsumer.processSessionChange)
- Session creation
- Session update
- Session deletion

#### 3. Manual Trending Calculation
- `POST /internal/v1/trending/events/{eventId}/calculate`
- `POST /internal/v1/trending/calculate-all`

#### 4. Scheduled Recalculation
- Runs every hour (configurable via `trending.update-schedule`)
- Automatically evicts cache after recalculation

### Session Count Cache Eviction
The `sessionCount` cache is evicted in the following scenarios:

#### 1. Event Changes (DebeziumEventConsumer.processEventChange)
- Event deletion
- Event approval
- Event completion
- Event status change

#### 2. Session Changes (DebeziumEventConsumer.processSessionChange)
- Session creation
- Session update
- Session deletion

## Implementation Details

### Cache Annotations
- `@EnableCaching` on `CacheConfig` class
- `@Cacheable` on service methods:
  - `EventTrendingServiceImpl.getTopTrendingEventThumbnails()`
  - `EventQueryService.countAllSessions()`

### Cache Manager Integration
The `CacheManager` is injected into:
- `DebeziumEventConsumer` - For Kafka event-driven cache eviction
- `TrendingScoreSchedulerConfig` - For scheduled cache eviction
- `TrendingController` - For manual calculation cache eviction

### Cache Eviction Methods
Helper methods implemented in relevant classes:
```java
private void evictTrendingCache() {
    if (cacheManager.getCache(TRENDING_EVENTS_CACHE) != null) {
        cacheManager.getCache(TRENDING_EVENTS_CACHE).clear();
        log.info("Evicted trending events cache");
    }
}

private void evictSessionCountCache() {
    if (cacheManager.getCache(SESSION_COUNT_CACHE) != null) {
        cacheManager.getCache(SESSION_COUNT_CACHE).clear();
        log.info("Evicted session count cache");
    }
}
```

## Kafka Topics Triggering Cache Eviction

Cache eviction is triggered by changes in the following Debezium CDC topics:

1. **dbz.ticketly.public.events**
   - Evicts: `trendingEvents`, `sessionCount`
   
2. **dbz.ticketly.public.event_sessions**
   - Evicts: `trendingEvents`, `sessionCount`

## Benefits

1. **Reduced Database Load**: Frequently accessed data is served from cache
2. **Improved Response Times**: Sub-millisecond cache retrieval vs. database queries
3. **Automatic Cache Invalidation**: Event-driven cache eviction ensures data freshness
4. **Scalability**: Redis can be clustered for high-availability scenarios
5. **Monitoring**: Cache hits/misses can be monitored via Spring Boot Actuator

## Monitoring

To monitor cache performance, use Spring Boot Actuator metrics:
- `cache.gets` - Total cache get operations
- `cache.hits` - Successful cache retrievals
- `cache.misses` - Cache misses requiring database access
- `cache.evictions` - Number of cache evictions

## Testing

To test the caching implementation:

1. Start Redis:
   ```bash
   docker run -d -p 6379:6379 redis:latest
   ```

2. Make a request to a cached endpoint
3. Check Redis for cached data:
   ```bash
   redis-cli
   KEYS event-seating-ms::*
   GET event-seating-ms::trendingEvents::10
   ```

4. Trigger a cache eviction by modifying events/sessions
5. Verify cache is cleared

## Configuration Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `REDIS_HOST` | localhost | Redis server hostname |
| `REDIS_PORT` | 6379 | Redis server port |
| `REDIS_TIMEOUT` | 2000 | Connection timeout in milliseconds |
| `trending.update-schedule` | 0 0 * * * * | Cron expression for trending score updates |

## Future Enhancements

1. **Cache Warming**: Pre-populate cache during application startup
2. **Distributed Caching**: Use Redis Cluster for high availability
3. **Cache Metrics Dashboard**: Integrate with Grafana for visualization
4. **Selective Cache Eviction**: Evict specific cache entries instead of clearing entire cache
5. **Cache Compression**: Enable Redis compression for large cache entries
