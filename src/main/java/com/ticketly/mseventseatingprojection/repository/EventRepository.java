package com.ticketly.mseventseatingprojection.repository;


import com.ticketly.mseventseatingprojection.model.EventDocument;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

/**
 * Reactive repository for the 'events' collection.
 */
@Repository
public interface EventRepository extends ReactiveMongoRepository<EventDocument, String> {
    /**
     * Finds an event by a session ID contained within its sessions array.
     * @param sessionId The ID of the session to find.
     * @return A Mono emitting the found EventDocument.
     */
    @Query("{ 'sessions.id': ?0 }")
    Mono<EventDocument> findEventIdBySessionId(String sessionId);

    /**
     * Performs a targeted update on a single session within an event document.
     * It finds the event by its ID and the specific session by its ID within the array,
     * then replaces the entire session object with the new one.
     *
     * @param eventId The ID of the parent event document.
     * @param sessionInfo The complete, updated SessionInfo object.
     * @return A Mono emitting the number of documents modified (should be 1).
     */
    @Query("{ '_id': ?0, 'sessions.id': ?1 }")
    Mono<Long> updateSessionInEvent(String eventId, EventDocument.SessionInfo sessionInfo);

    /**
     * Performs a targeted update on the seating map of a single session within an event document.
     * This is a highly efficient operation that only modifies the nested layoutData field.
     *
     * @param eventId The ID of the parent event document.
     * @param sessionId The ID of the session to update.
     * @param seatingMapInfo The new, complete SessionSeatingMapInfo object.
     * @return A Mono emitting the number of documents modified (should be 1).
     */
    @Query("{ '_id': ?0 }")
    Mono<Long> updateSeatingMapInSession(String eventId, String sessionId, EventDocument.SessionSeatingMapInfo seatingMapInfo);
}
