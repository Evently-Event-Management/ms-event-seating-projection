package com.ticketly.mseventseatingprojection.repository;

import com.ticketly.mseventseatingprojection.model.EventTrackingDocument;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

@Repository
public interface EventViewsRepository extends ReactiveMongoRepository<EventTrackingDocument, String> {
    
    /**
     * Find analytics document by eventId and date
     *
     * @param eventId Event ID
     * @param date Date for the analytics
     * @return Mono of EventTrackingDocument
     */
    Mono<EventTrackingDocument> findByEventIdAndTrackingBucketDate(String eventId, LocalDate date);
    
    /**
     * Find all analytics documents for a specific event
     *
     * @param eventId Event ID
     * @return Flux of EventAnalyticsDocument
     */
    Flux<EventTrackingDocument> findByEventId(String eventId);
    
    /**
     * Delete all analytics for a specific event
     *
     * @param eventId Event ID
     * @return Mono of Void
     */
    Mono<Void> deleteByEventId(String eventId);
}