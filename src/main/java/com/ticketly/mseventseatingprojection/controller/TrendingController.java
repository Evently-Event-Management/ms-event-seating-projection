package com.ticketly.mseventseatingprojection.controller;

import com.ticketly.mseventseatingprojection.model.EventTrendingDocument;
import com.ticketly.mseventseatingprojection.service.EventTrendingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Objects;

import static com.ticketly.mseventseatingprojection.config.CacheConfig.TRENDING_EVENTS_CACHE;

@RestController
@RequestMapping("/internal/v1/trending")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('SCOPE_internal-api')")
@Slf4j
public class TrendingController {

    private final EventTrendingService eventTrendingService;
    private final CacheManager cacheManager;

    /**
     * Internal endpoint to get trending score for a specific event
     *
     * @param eventId The ID of the event
     * @return Event trending data
     */
    @GetMapping("/events/{eventId}")
    public Mono<ResponseEntity<EventTrendingDocument>> getEventTrendingScore(@PathVariable String eventId) {
        log.info("Getting trending score for eventId={}", eventId);
        
        return eventTrendingService.getEventTrendingScore(eventId)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /**
     * Internal endpoint to calculate and update trending score for a specific event
     * Evicts the trending cache to ensure fresh data
     *
     * @param eventId The ID of the event
     * @return Updated event trending data
     */
    @PostMapping("/events/{eventId}/calculate")
    public Mono<ResponseEntity<EventTrendingDocument>> calculateEventTrendingScore(@PathVariable String eventId) {
        log.info("Calculating trending score for eventId={}", eventId);
        
        return eventTrendingService.calculateAndUpdateTrendingScore(eventId)
                .doOnSuccess(doc -> evictTrendingCache())
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /**
     * Internal endpoint to calculate trending scores for all events
     * Evicts the trending cache after calculation
     *
     * @return Updated event trending data for all events
     */
    @PostMapping("/calculate-all")
    public Flux<EventTrendingDocument> calculateAllTrendingScores() {
        log.info("Calculating trending scores for all events");
        
        return eventTrendingService.calculateAndUpdateAllTrendingScores()
                .doOnComplete(this::evictTrendingCache);
    }

    /**
     * Evicts the trending events cache
     */
    private void evictTrendingCache() {
        try {
            if (cacheManager.getCache(TRENDING_EVENTS_CACHE) != null) {
                Objects.requireNonNull(cacheManager.getCache(TRENDING_EVENTS_CACHE)).clear();
                log.info("Evicted trending events cache after manual calculation");
            }
        } catch (Exception e) {
            log.error("Error evicting trending cache: {}", e.getMessage());
        }
    }

    /**
     * Get top trending events
     *
     * @param limit Optional limit parameter (default 10)
     * @return List of top trending events
     */
    @GetMapping("/top")
    public Flux<EventTrendingDocument> getTopTrendingEvents(
            @RequestParam(name = "limit", defaultValue = "10") int limit) {
        log.info("Getting top {} trending events", limit);
        
        return eventTrendingService.getTopTrendingEvents(limit);
    }
}