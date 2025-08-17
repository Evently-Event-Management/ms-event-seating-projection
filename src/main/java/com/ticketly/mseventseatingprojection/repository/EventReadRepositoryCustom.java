package com.ticketly.mseventseatingprojection.repository;

import com.ticketly.mseventseatingprojection.model.EventDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;


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

    Mono<EventDocument> findEventBySessionId(String sessionId);
}
