package com.ticketly.mseventseatingprojection.repository;

import com.ticketly.mseventseatingprojection.model.EventDocument;
import reactor.core.publisher.Mono;

/**
 * Repository interface for event analytics operations
 */
public interface EventAnalyticsRepository {

    /**
     * Find complete event document with all sessions and seating data for analytics
     * @param eventId The ID of the event to analyze
     * @return A Mono containing the complete event document
     */
    Mono<EventDocument> findEventWithCompleteSeatingData(String eventId);

    /**
     * Find a specific session with complete seating data for analytics
     * @param eventId The ID of the event
     * @param sessionId The ID of the session to analyze
     * @return A Mono containing the event document with the specified session
     */
    Mono<EventDocument> findSessionWithCompleteSeatingData(String eventId, String sessionId);
}
