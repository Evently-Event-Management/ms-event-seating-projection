package com.ticketly.mseventseatingprojection.repository;

import com.ticketly.mseventseatingprojection.dto.read.SessionStatusInfo;
import com.ticketly.mseventseatingprojection.model.EventDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;

@Repository
public interface EventReadRepositoryCustom {

    /**
     * Performs a complex search for events with multiple optional filters.
     *
     * @param searchTerm Search keyword for event title or description.
     * @param categoryId Category ID to filter events.
     * @param longitude  Longitude for location-based search.
     * @param latitude   Latitude for location-based search.
     * @param radiusKm   Radius in kilometers for location-based search.
     * @param dateFrom   Start date filter.
     * @param dateTo     End date filter.
     * @param priceMin   Minimum price filter.
     * @param priceMax   Maximum price filter.
     * @param pageable   Pagination information.
     * @return Mono emitting a page of EventDocument.
     */
    Mono<Page<EventDocument>> searchEvents(
            String searchTerm,
            String categoryId,
            Double longitude,
            Double latitude,
            Integer radiusKm,
            Instant dateFrom,
            Instant dateTo,
            BigDecimal priceMin,
            BigDecimal priceMax,
            Pageable pageable
    );

    /**
     * Builds a Criteria object for category filtering, including subcategories if present.
     *
     * @param categoryId The category ID to filter by.
     * @return Mono emitting the Criteria for category filtering.
     */
    Mono<Criteria> getCategoryCriteria(String categoryId);

    /**
     * Executes the aggregation pipeline for event search with all filters applied.
     *
     * @param searchTerm      Search keyword.
     * @param categoryCriteria Criteria for category filtering.
     * @param longitude       Longitude for location-based search.
     * @param latitude        Latitude for location-based search.
     * @param radiusKm        Radius in kilometers for location-based search.
     * @param dateFrom        Start date filter.
     * @param dateTo          End date filter.
     * @param priceMin        Minimum price filter.
     * @param priceMax        Maximum price filter.
     * @param pageable        Pagination information.
     * @return Mono emitting a page of EventDocument.
     */
    Mono<Page<EventDocument>> executeAggregation(
            String searchTerm, Criteria categoryCriteria, Double longitude, Double latitude,
            Integer radiusKm, Instant dateFrom, Instant dateTo,
            BigDecimal priceMin, BigDecimal priceMax, Pageable pageable);

    /**
     * Finds the event document containing a session by session ID, excluding layout data.
     *
     * @param sessionId The session ID to search for.
     * @return Mono emitting the EventDocument with session info.
     */
    Mono<EventDocument> findSessionBasicInfoById(String sessionId);

    /**
     * Finds basic event info by event ID, excluding sessions.
     *
     * @param eventId The event ID to search for.
     * @return Mono emitting the EventDocument with basic info.
     */
    Mono<EventDocument> findEventBasicInfoById(String eventId);

    /**
     * Finds sessions for a given event, paginated.
     *
     * @param eventId  The event ID.
     * @param pageable Pagination information.
     * @return Mono emitting a page of SessionInfo.
     */
    Mono<Page<EventDocument.SessionInfo>> findSessionsByEventId(String eventId, Pageable pageable);

    /**
     * Finds the seating map for a specific session by its ID.
     *
     * @param sessionId The ID of the session
     * @return A Mono emitting the SessionSeatingMapInfo for the given session or empty if not found
     */
    Mono<EventDocument.SessionSeatingMapInfo> findSeatingMapBySessionId(String sessionId);

    /**
     * Finds sessions for a given event within a date range.
     *
     * @param eventId  The event ID.
     * @param fromDate Start date.
     * @param toDate   End date.
     * @return Flux emitting SessionInfo for each session in range.
     */
    Flux<EventDocument.SessionInfo> findSessionsInRange(String eventId, Instant fromDate, Instant toDate);

    /**
     * Finds the status of a session by its ID.
     *
     * @param sessionId The session ID.
     * @return Mono emitting SessionStatusInfo for the session.
     */
    Mono<SessionStatusInfo> findSessionStatusById(String sessionId);

    /**
     * Finds all public discounts for a specific event and session.
     *
     * @param eventId The event ID.
     * @param sessionId The session ID.
     * @return Flux emitting DiscountInfo for each public discount.
     */
    Flux<EventDocument.DiscountInfo> findPublicDiscountsByEventAndSession(String eventId, String sessionId);

    /**
     * Finds all public discounts for a specific event.
     *
     * @param eventId The event ID.
     * @return Flux emitting DiscountInfo for each public discount.
     */
    Flux<EventDocument.DiscountInfo> findPublicDiscountsByEvent(String eventId);


    /**
     * Finds an active discount by its code for a specific event and session.
     *
     * @param eventId The event ID.
     * @param sessionId The session ID.
     * @param code The discount code.
     * @return Mono emitting the DiscountInfo if found and active, otherwise empty.
     */
    Mono<EventDocument.DiscountInfo> findActiveDiscountByCodeAndEventAndSession(String eventId, String sessionId, String code);

    /**
     * Finds an active discount by its code for a specific session.
     *
     * @param sessionId The session ID.
     * @param code The discount code.
     * @return Mono emitting the DiscountInfo if found and active, otherwise empty.
     */
    Mono<EventDocument.DiscountInfo> findActiveDiscountByCodeAndSession(String sessionId, String code);
    
    /**
     * Counts the total number of sessions across all events in the database.
     *
     * @return Mono emitting the total count of sessions.
     */
    Mono<Long> countAllSessions();
}