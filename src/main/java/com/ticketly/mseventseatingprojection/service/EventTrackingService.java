package com.ticketly.mseventseatingprojection.service;

import com.ticketly.mseventseatingprojection.dto.analytics.EventViewsStatsDTO;
import com.ticketly.mseventseatingprojection.model.EventTrackingDocument;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

/**
 * Service interface for tracking event views and orders
 */
public interface EventTrackingService {

    /**
     * Increment the view count for an event on the current date by device type
     * 
     * @param eventId Event ID
     * @param deviceType Device type (mobile, desktop, tablet)
     * @return Updated EventTrackingDocument
     */
    Mono<EventTrackingDocument> incrementViewCount(String eventId, String deviceType);
    
    /**
     * Increment the order count for an event on the current date
     * 
     * @param eventId Event ID
     * @return Updated EventAnalyticsDocument
     */
    Mono<EventTrackingDocument> incrementOrderCount(String eventId);
    
    /**
     * Get event views statistics for a specific date range
     * 
     * @param eventId Event ID
     * @param fromDate Start date (inclusive)
     * @param toDate End date (inclusive)
     * @return DTO with aggregated statistics
     */
    Mono<EventViewsStatsDTO> getEventViewsStats(String eventId, LocalDate fromDate, LocalDate toDate);
    
    /**
     * Get all analytics data for an event
     * 
     * @param eventId Event ID
     * @return Flux of EventAnalyticsDocument
     */
    Flux<EventTrackingDocument> getAllEventAnalytics(String eventId);
    
    /**
     * Delete all analytics for a specific event
     * 
     * @param eventId Event ID
     * @return Mono of Void
     */
    Mono<Void> deleteEventAnalytics(String eventId);
}