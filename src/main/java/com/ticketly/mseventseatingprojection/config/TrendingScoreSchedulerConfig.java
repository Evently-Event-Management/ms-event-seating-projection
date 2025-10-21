package com.ticketly.mseventseatingprojection.config;

import com.ticketly.mseventseatingprojection.service.EventTrendingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.Objects;

import static com.ticketly.mseventseatingprojection.config.CacheConfig.TRENDING_EVENTS_CACHE;

@Configuration
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class TrendingScoreSchedulerConfig {

    private final EventTrendingService eventTrendingService;
    private final CacheManager cacheManager;
    
    /**
     * Scheduler to update trending scores for all events every hour
     * Also evicts the trending cache to ensure fresh data is served
     */
    @Scheduled(cron = "${trending.update-schedule:0 0 * * * *}") // Default: Every hour
    public void updateAllTrendingScores() {
        log.info("Scheduled job: Updating trending scores for all events");
        eventTrendingService.calculateAndUpdateAllTrendingScores()
                .count()
                .subscribe(
                        count -> {
                            log.info("Updated trending scores for {} events", count);
                            evictTrendingCache();
                        },
                        error -> log.error("Error updating trending scores: {}", error.getMessage(), error)
                );
    }

    /**
     * Evicts the trending events cache after recalculation
     */
    private void evictTrendingCache() {
        try {
            if (cacheManager.getCache(TRENDING_EVENTS_CACHE) != null) {
                Objects.requireNonNull(cacheManager.getCache(TRENDING_EVENTS_CACHE)).clear();
                log.info("Evicted trending events cache after scheduled recalculation");
            }
        } catch (Exception e) {
            log.error("Error evicting trending cache: {}", e.getMessage());
        }
    }
}