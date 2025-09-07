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

    Mono<Criteria> getCategoryCriteria(String categoryId);

    Mono<Page<EventDocument>> executeAggregation(
            String searchTerm, Criteria categoryCriteria, Double longitude, Double latitude,
            Integer radiusKm, Instant dateFrom, Instant dateTo,
            BigDecimal priceMin, BigDecimal priceMax, Pageable pageable);

    Mono<EventDocument> findSessionBasicInfoById(String sessionId);

    Mono<EventDocument> findEventBasicInfoById(String eventId);

    Mono<Page<EventDocument.SessionInfo>> findSessionsByEventId(String eventId, Pageable pageable);

    /**
     * Finds the seating map for a specific session by its ID.
     *
     * @param sessionId The ID of the session
     * @return A Mono emitting the SessionSeatingMapInfo for the given session or empty if not found
     */
    Mono<EventDocument.SessionSeatingMapInfo> findSeatingMapBySessionId(String sessionId);

    Flux<EventDocument.SessionInfo> findSessionsInRange(String eventId, Instant fromDate, Instant toDate);

    Mono<SessionStatusInfo> findSessionStatusById(String sessionId);
}