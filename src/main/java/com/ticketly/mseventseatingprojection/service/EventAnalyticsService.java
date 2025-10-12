package com.ticketly.mseventseatingprojection.service;

import com.ticketly.mseventseatingprojection.dto.analytics.EventAnalyticsDTO;
import com.ticketly.mseventseatingprojection.dto.analytics.SessionAnalyticsDTO;
import com.ticketly.mseventseatingprojection.dto.analytics.SessionSummaryDTO;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.List;

/**
 * Service interface for event analytics operations
 */
public interface EventAnalyticsService {

    /**
     * Get comprehensive analytics for an entire event
     * @param eventId The ID of the event to analyze
     * @return Event analytics data
     */
    Mono<EventAnalyticsDTO> getEventAnalytics(String eventId);

    /**
     * Get comprehensive analytics for an entire event with user context
     * @param eventId The ID of the event to analyze
     * @param userId The ID of the user requesting the analytics
     * @return Event analytics data
     */
    Mono<EventAnalyticsDTO> getEventAnalytics(String eventId, String userId);

    /**
     * Get analytics for a specific session within an event
     * @param eventId The ID of the event
     * @param sessionId The ID of the session to analyze
     * @return Session analytics data
     */
    Mono<SessionAnalyticsDTO> getSessionAnalytics(String eventId, String sessionId);

    /**
     * Get analytics for a specific session within an event with user context
     * @param eventId The ID of the event
     * @param sessionId The ID of the session to analyze
     * @param userId The ID of the user requesting the analytics
     * @return Session analytics data
     */
    Mono<SessionAnalyticsDTO> getSessionAnalytics(String eventId, String sessionId, String userId);

    /**
     * Get basic analytics for all sessions in an event
     * @param eventId The ID of the event
     * @return Flux of session summary data
     */
    Flux<SessionSummaryDTO> getAllSessionsAnalytics(String eventId);

    /**
     * Get basic analytics for all sessions in an event with user context
     * @param eventId The ID of the event
     * @param userId The ID of the user requesting the analytics
     * @return Flux of session summary data
     */
    Flux<SessionSummaryDTO> getAllSessionsAnalytics(String eventId, String userId);

    /**
     * Get analytics for multiple events in batch
     * @param eventIds List of event IDs to analyze
     * @return Flux of event analytics data
     */
    Flux<EventAnalyticsDTO> getBatchEventAnalytics(List<String> eventIds);

    /**
     * Get analytics for multiple events in batch with user context
     * @param eventIds List of event IDs to analyze
     * @param userId The ID of the user requesting the analytics
     * @return Flux of event analytics data
     */
    Flux<EventAnalyticsDTO> getBatchEventAnalytics(List<String> eventIds, String userId);
}
