package com.ticketly.mseventseatingprojection.repository;

import com.ticketly.mseventseatingprojection.model.EventTrackingDocument;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
// Renamed to match your Document model for consistency
public interface EventTrackingRepository extends ReactiveMongoRepository<EventTrackingDocument, String> {

    /**
     * Find the single analytics document for a specific event.
     * There should only ever be one.
     *
     * @param eventId Event ID
     * @return Mono of EventTrackingDocument
     */
    Mono<EventTrackingDocument> findByEventId(String eventId);

    /**
     * Delete all analytics for a specific event
     *
     * @param eventId Event ID
     * @return Mono of Void
     */
    Mono<Void> deleteByEventId(String eventId);
}