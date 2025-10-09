package com.ticketly.mseventseatingprojection.service;

import com.ticketly.mseventseatingprojection.model.EventTrackingDocument;
import reactor.core.publisher.Mono;


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
     * Delete all analytics for a specific event
     * 
     * @param eventId Event ID
     * @return Mono of Void
     */
    Mono<Void> deleteEventAnalytics(String eventId);
}