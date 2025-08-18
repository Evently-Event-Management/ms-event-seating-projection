package com.ticketly.mseventseatingprojection.repository;

import com.ticketly.mseventseatingprojection.model.CategoryDocument;
import com.ticketly.mseventseatingprojection.model.EventDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.TypedAggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.NearQuery;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.TextCriteria;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class EventReadRepositoryCustomImpl implements EventReadRepositoryCustom {

    private final ReactiveMongoTemplate reactiveMongoTemplate;
    private final CategoryReadRepository categoryReadRepository;

    @Override
    public Mono<Page<EventDocument>> searchEvents(
            String searchTerm, String categoryId, Double longitude, Double latitude,
            Integer radiusKm, Instant dateFrom, Instant dateTo,
            BigDecimal priceMin, BigDecimal priceMax, Pageable pageable
    ) {
        if (categoryId != null) {
            return getCategoryCriteria(categoryId)
                    .flatMap(categoryCriteria -> executeAggregation(searchTerm, categoryCriteria, longitude, latitude,
                            radiusKm, dateFrom, dateTo, priceMin, priceMax, pageable));
        }
        return executeAggregation(searchTerm, null, longitude, latitude,
                radiusKm, dateFrom, dateTo, priceMin, priceMax, pageable);
    }

    private Mono<Criteria> getCategoryCriteria(String categoryId) {
        // This method remains unchanged.
        return categoryReadRepository.findByParentId(categoryId)
                .collectList()
                .map(subcategories -> {
                    if (!subcategories.isEmpty()) {
                        List<String> allCategoryIds = new ArrayList<>();
                        allCategoryIds.add(categoryId);
                        allCategoryIds.addAll(subcategories.stream().map(CategoryDocument::getId).toList());
                        return Criteria.where("category.id").in(allCategoryIds);
                    } else {
                        return Criteria.where("category.id").is(categoryId);
                    }
                });
    }

    private Mono<Page<EventDocument>> executeAggregation(
            String searchTerm, Criteria categoryCriteria, Double longitude, Double latitude,
            Integer radiusKm, Instant dateFrom, Instant dateTo,
            BigDecimal priceMin, BigDecimal priceMax, Pageable pageable) {

        List<AggregationOperation> pipeline = new ArrayList<>();
        List<Criteria> matchCriteriaList = new ArrayList<>();

        // CRITICAL: $text search MUST be the first stage if used
        boolean hasTextSearch = searchTerm != null && !searchTerm.isBlank();
        boolean hasGeoSearch = longitude != null && latitude != null && radiusKm != null;

        if (hasTextSearch && hasGeoSearch) {
            // OPTION 1: Use $text first, then filter by location in $match stage
            pipeline.add(Aggregation.match(TextCriteria.forDefaultLanguage().matching(searchTerm)));

            // Use $geoWithin $centerSphere instead of $nearSphere in aggregation
            Point userLocation = new Point(longitude, latitude);
            Distance radius = new Distance(radiusKm, Metrics.KILOMETERS);
            Circle circle = new Circle(userLocation, radius);

            matchCriteriaList.add(Criteria.where("sessions.venueDetails.location")
                    .withinSphere(circle));

        } else if (hasTextSearch) {
            // Only text search - must be first stage
            pipeline.add(Aggregation.match(TextCriteria.forDefaultLanguage().matching(searchTerm)));
        } else if (hasGeoSearch) {
            // Only geo search - can use $geoNear for distance calculation
            Point userLocation = new Point(longitude, latitude);
            Distance radius = new Distance(radiusKm, Metrics.KILOMETERS);
            NearQuery nearQuery = NearQuery.near(userLocation).spherical(true).maxDistance(radius);
            pipeline.add(Aggregation.geoNear(nearQuery, "distance"));
        }

        // Add other match criteria
        if (categoryCriteria != null) {
            matchCriteriaList.add(categoryCriteria);
        }

        if (dateFrom != null || dateTo != null) {
            Criteria dateCriteria = Criteria.where("sessions.startTime");
            if (dateFrom != null) dateCriteria.gte(dateFrom);
            if (dateTo != null) dateCriteria.lte(dateTo);
            matchCriteriaList.add(dateCriteria);
        }

        if (priceMin != null || priceMax != null) {
            Criteria priceCriteria = Criteria.where("tiers.price");
            if (priceMin != null) priceCriteria.gte(priceMin);
            if (priceMax != null) priceCriteria.lte(priceMax);
            matchCriteriaList.add(priceCriteria);
        }

        matchCriteriaList.add(Criteria.where("status").is("APPROVED"));

        // Add remaining match criteria as a single $match stage
        // Removed redundant check: matchCriteriaList will always have at least the status criteria
        pipeline.add(Aggregation.match(new Criteria().andOperator(matchCriteriaList)));

        // Rest of the pipeline remains the same
        List<AggregationOperation> countPipelineOps = new ArrayList<>(pipeline);
        countPipelineOps.add(Aggregation.count().as("total"));
        TypedAggregation<Map> countAggregation = Aggregation.newAggregation(Map.class, countPipelineOps);

        Mono<Long> countMono = reactiveMongoTemplate.aggregate(countAggregation, "events", Map.class)
                .singleOrEmpty()
                .map(map -> {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> typedMap = (Map<String, Object>) map;
                    return Long.parseLong(typedMap.getOrDefault("total", 0).toString());
                })
                .defaultIfEmpty(0L);

        if (pageable.getSort() != Sort.unsorted()) {
            pipeline.add(Aggregation.sort(pageable.getSort()));
        }
        pipeline.add(Aggregation.skip(pageable.getOffset()));
        pipeline.add(Aggregation.limit(pageable.getPageSize()));

        TypedAggregation<EventDocument> finalAggregation = Aggregation.newAggregation(EventDocument.class, pipeline);

        return reactiveMongoTemplate.aggregate(finalAggregation, "events", EventDocument.class)
                .collectList()
                .zipWith(countMono)
                .map(tuple -> new PageImpl<>(tuple.getT1(), pageable, tuple.getT2()));
    }

    // findEventBySessionId remains unchanged.
    @Override
    public Mono<EventDocument> findEventBySessionId(String sessionId) {
        return reactiveMongoTemplate.findOne(
                new Query(Criteria.where("sessions.id").is(sessionId)),
                EventDocument.class
        );
    }
}