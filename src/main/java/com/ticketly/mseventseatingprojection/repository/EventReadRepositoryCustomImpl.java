package com.ticketly.mseventseatingprojection.repository;

import com.ticketly.mseventseatingprojection.model.EventDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.TextCriteria;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class EventReadRepositoryCustomImpl implements EventReadRepositoryCustom {

    private final ReactiveMongoTemplate reactiveMongoTemplate;

    @Override
    public Mono<Page<EventDocument>> searchEvents(
            String searchTerm, String categoryId, Double longitude, Double latitude,
            Integer radiusKm, Instant dateFrom, Instant dateTo,
            BigDecimal priceMin, BigDecimal priceMax, Pageable pageable
    ) {
        Query query = new Query().with(pageable);
        List<Criteria> criteriaList = new ArrayList<>();

        // 1. Full-Text Search
        if (searchTerm != null && !searchTerm.isBlank()) {
            query.addCriteria(TextCriteria.forDefaultLanguage().matching(searchTerm));
        }

        // 2. Category Filter
        if (categoryId != null) {
            criteriaList.add(Criteria.where("category.id").is(categoryId));
        }

        // 3. Date Range Filter
        if (dateFrom != null || dateTo != null) {
            Criteria dateCriteria = Criteria.where("sessions.startTime");
            if (dateFrom != null) dateCriteria.gte(dateFrom);
            if (dateTo != null) dateCriteria.lte(dateTo);
            criteriaList.add(dateCriteria);
        }

        // 4. Price Range Filter
        if (priceMin != null || priceMax != null) {
            Criteria priceCriteria = Criteria.where("tiers.price");
            if (priceMin != null) priceCriteria.gte(priceMin);
            if (priceMax != null) priceCriteria.lte(priceMax);
            criteriaList.add(priceCriteria);
        }

        // 5. Geospatial (Location) Filter
        if (longitude != null && latitude != null && radiusKm != null) {
            GeoJsonPoint userLocation = new GeoJsonPoint(longitude, latitude);
            Distance radius = new Distance(radiusKm, Metrics.KILOMETERS);
            criteriaList.add(Criteria.where("sessions.venueDetails.location").nearSphere(userLocation).maxDistance(radius.getValue()));
        }

        // Always filter for APPROVED events
        criteriaList.add(Criteria.where("status").is("APPROVED"));

        if (!criteriaList.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
        }

        Mono<Long> countMono = reactiveMongoTemplate.count(query, EventDocument.class);
        return reactiveMongoTemplate.find(query, EventDocument.class)
                .collectList()
                .zipWith(countMono)
                .map(tuple -> new PageImpl<>(tuple.getT1(), pageable, tuple.getT2()));
    }

    // Implementation for finding event by sessionId
    @Override
    public Mono<EventDocument> findEventBySessionId(String sessionId) {
        Query query = new Query();
        query.addCriteria(Criteria.where("sessions.id").is(sessionId));
        return reactiveMongoTemplate.findOne(query, EventDocument.class);
    }
}
