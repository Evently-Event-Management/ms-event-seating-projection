package com.ticketly.mseventseatingprojection.repository;

import com.ticketly.mseventseatingprojection.dto.analytics.BlockOccupancyDTO;
import com.ticketly.mseventseatingprojection.dto.analytics.SessionSummaryDTO;
import com.ticketly.mseventseatingprojection.dto.analytics.TierSalesDTO;
import com.ticketly.mseventseatingprojection.dto.analytics.raw.EventOverallStatsDTO;
import com.ticketly.mseventseatingprojection.dto.analytics.raw.SeatStatusCountDTO;
import com.ticketly.mseventseatingprojection.dto.analytics.raw.SessionStatusCountDTO;
import com.ticketly.mseventseatingprojection.model.EventDocument;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Repository interface for event analytics operations
 */
public interface EventAnalyticsRepository {

    /**
     * Find complete event document with all sessions and seating data for analytics
     *
     * @param eventId The ID of the event to analyze
     * @return A Mono containing the complete event document
     */
    Mono<EventDocument> findEventWithCompleteSeatingData(String eventId);

    /**
     * Find a specific session with complete seating data for analytics
     *
     * @param eventId   The ID of the event
     * @param sessionId The ID of the session to analyze
     * @return A Mono containing the event document with the specified session
     */
    Mono<EventDocument> findSessionWithCompleteSeatingData(String eventId, String sessionId);

    /**
     * Get overall event statistics using aggregation
     *
     * @param eventId The ID of the event to analyze
     * @return A Mono containing the overall event statistics
     */
    Mono<EventOverallStatsDTO> getEventOverallStats(String eventId);

    /**
     * Get session status counts for an event using aggregation
     *
     * @param eventId The ID of the event to analyze
     * @return A Flux containing the count of sessions by status
     */
    Flux<SessionStatusCountDTO> getSessionStatusCounts(String eventId);

    /**
     * Get tier-based sales analytics for an event using aggregation
     *
     * @param eventId The ID of the event to analyze
     * @return A Flux containing tier analytics data
     */
    Flux<TierSalesDTO> getTierAnalytics(String eventId);

    /**
     * Get tier-based sales analytics for an event using aggregation
     *
     * @param eventId The ID of the event to analyze
     * @param sessionId The ID of the session to analyze
     * @return A Flux containing tier analytics data
     */
    Flux<TierSalesDTO> getTierAnalytics(String eventId, String sessionId);


    /**
     * Get summaries for all sessions of an event using aggregation
     *
     * @param eventId The ID of the event to analyze
     * @return A Flux containing summaries for all sessions
     */
    Flux<SessionSummaryDTO> getAllSessionsAnalytics(String eventId);

    /**
     * Get summary for a specific session of an event using aggregation
     *
     * @param eventId   The ID of the event
     * @param sessionId The ID of the session to analyze
     * @return A Mono containing the session summary
     */
    Mono<SessionSummaryDTO> getSessionSummary(String eventId, String sessionId);

    /**
     * Get session status counts for an event using aggregation
     *
     * @param eventId The ID of the event to analyze
     * @param sessionId The ID of the session to analyze
     * @return A Flux containing the count of sessions by status
     */
    Flux<SeatStatusCountDTO> getSessionStatusCounts(String eventId, String sessionId);

    /**
     * Get block occupancy details for a specific session in an event
     *
     * @param eventId   The ID of the event
     * @param sessionId The ID of the session to analyze
     * @return A Flux containing block occupancy details
     */
    Flux<BlockOccupancyDTO> getBlockOccupancy(String eventId, String sessionId);
}
