package com.ticketly.mseventseatingprojection.service;

import com.ticketly.mseventseatingprojection.dto.analytics.EventAnalyticsDTO;
import com.ticketly.mseventseatingprojection.dto.analytics.SessionAnalyticsDTO;
import com.ticketly.mseventseatingprojection.dto.analytics.SessionSummaryDTO;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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
     * Get analytics for a specific session within an event
     * @param eventId The ID of the event
     * @param sessionId The ID of the session to analyze
     * @return Session analytics data
     */
    Mono<SessionAnalyticsDTO> getSessionAnalytics(String eventId, String sessionId);

    /**
     * Get basic analytics for all sessions in an event
     * @param eventId The ID of the event
     * @return Flux of session summary data
     */
    Flux<SessionSummaryDTO> getAllSessionsAnalytics(String eventId);
}
