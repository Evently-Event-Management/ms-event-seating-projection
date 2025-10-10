package com.ticketly.mseventseatingprojection.service;

import com.ticketly.mseventseatingprojection.dto.analytics.EventViewsDTO;
import reactor.core.publisher.Mono;

/**
 * Service interface for Google Analytics operations
 */
public interface GoogleAnalyticsService {

    /**
     * Get the total view count for a specific event from Google Analytics
     * @param eventId The ID of the event to get views for
     * @return View count data
     */
    Mono<Integer> getEventTotalViews(String eventId);
    
    /**
     * Get all analytics details for an event from Google Analytics
     * @param eventId The ID of the event to analyze
     * @return Event views analytics data
     */
    Mono<EventViewsDTO> getEventViewsAnalytics(String eventId);
}