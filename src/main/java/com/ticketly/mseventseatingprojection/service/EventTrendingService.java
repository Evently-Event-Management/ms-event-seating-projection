package com.ticketly.mseventseatingprojection.service;

import com.ticketly.mseventseatingprojection.model.EventTrendingDocument;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Service interface for event trending operations
 */
public interface EventTrendingService {

    /**
     * Get the trending score for a specific event
     * @param eventId The ID of the event to get trending score for
     * @return The event trending data
     */
    Mono<EventTrendingDocument> getEventTrendingScore(String eventId);
    
    /**
     * Calculate and update trending score for a specific event
     * @param eventId The ID of the event to calculate trending score for
     * @return The updated event trending document
     */
    Mono<EventTrendingDocument> calculateAndUpdateTrendingScore(String eventId);
    
    /**
     * Calculate and update trending scores for all events
     * @return Flux of updated event trending documents
     */
    Flux<EventTrendingDocument> calculateAndUpdateAllTrendingScores();
    
    /**
     * Get top trending events up to a specified limit
     * @param limit Maximum number of trending events to return
     * @return Flux of trending events sorted by score
     */
    Flux<EventTrendingDocument> getTopTrendingEvents(int limit);
    
    /**
     * Get top trending events as EventThumbnailDTO up to a specified limit
     * @param limit Maximum number of events to return
     * @return Flux of EventThumbnailDTO objects sorted by trending score
     */
    Flux<com.ticketly.mseventseatingprojection.dto.read.EventThumbnailDTO> getTopTrendingEventThumbnails(int limit);
}