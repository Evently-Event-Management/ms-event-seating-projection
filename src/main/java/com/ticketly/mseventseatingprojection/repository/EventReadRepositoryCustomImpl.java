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
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.NearQuery;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.TextCriteria;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
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

    @Override
    public Mono<EventDocument> findEventById(String eventId) {
        Query query = new Query(Criteria.where("id").is(eventId).and("status").is("APPROVED"));
        return reactiveMongoTemplate.findOne(query, EventDocument.class);
    }

    @Override
    public Mono<Page<EventDocument.SessionInfo>> findSessionsByEventId(String eventId, Pageable pageable) {
        // --- Aggregation Pipeline for Sessions ---

        // Stage 1: Match the parent event document by its ID.
        AggregationOperation matchEvent = Aggregation.match(Criteria.where("_id").is(eventId));

        // Stage 2: Deconstruct the sessions array into a stream of documents.
        AggregationOperation unwindSessions = Aggregation.unwind("sessions");

        // Stage 3: Promote the session sub-document to the root level.
        AggregationOperation replaceRoot = Aggregation.replaceRoot("sessions");

        // Stage 4: Project the fields, explicitly excluding layoutData.
        AggregationOperation projectFields = Aggregation.project(
                "id", "startTime", "endTime", "status", "sessionType", "venueDetails", "salesStartTime"
        );

        // --- COUNTING ---
        // To get the total count of sessions for pagination, we find the document and get the array size.
        // This is more efficient than a separate count aggregation.
        Mono<Long> countMono = reactiveMongoTemplate.findById(eventId, EventDocument.class)
                .map(event -> (long) event.getSessions().size())
                .defaultIfEmpty(0L);

        // --- EXECUTION ---
        // Build the pipeline for fetching the paginated data.
        TypedAggregation<EventDocument.SessionInfo> aggregation = Aggregation.newAggregation(
                EventDocument.SessionInfo.class,
                matchEvent,
                unwindSessions,
                replaceRoot,
                projectFields,
                Aggregation.sort(pageable.getSort()),
                Aggregation.skip(pageable.getOffset()),
                Aggregation.limit(pageable.getPageSize())
        );

        // Execute the aggregation and combine with the count to create the Page object.
        return reactiveMongoTemplate.aggregate(aggregation, "events", EventDocument.SessionInfo.class)
                .collectList()
                .zipWith(countMono)
                .map(tuple -> new PageImpl<>(tuple.getT1(), pageable, tuple.getT2()));
    }

    @Override
    public Mono<EventDocument.SessionSeatingMapInfo> findSeatingMapBySessionId(String sessionId) {
        return reactiveMongoTemplate.findOne(
                        new Query(Criteria.where("sessions.id").is(sessionId)),
                        EventDocument.class
                )
                .flatMap(eventDocument -> {
                    // Find the specific session within the event document
                    return Mono.justOrEmpty(eventDocument.getSessions().stream()
                            .filter(s -> s.getId().equals(sessionId))
                            .findFirst()
                            .map(EventDocument.SessionInfo::getLayoutData));
                });
    }

    @Override
    public Flux<EventDocument.SessionInfo> findSessionsInRange(String eventId, Instant fromDate, Instant toDate) {
        // Match the event by ID
        AggregationOperation matchEvent = Aggregation.match(Criteria.where("_id").is(eventId));

        // Unwind the sessions array
        AggregationOperation unwindSessions = Aggregation.unwind("sessions");

        // Match sessions within the date range
        AggregationOperation matchDateRange = Aggregation.match(Criteria.where("sessions.startTime").gte(fromDate).lte(toDate));

        // Replace root to promote session sub-document
        AggregationOperation replaceRoot = Aggregation.replaceRoot("sessions");

        // Project only necessary fields for SessionInfoDTO
        AggregationOperation projectFields = Aggregation.project("id", "startTime", "endTime", "status", "sessionType", "venueDetails", "salesStartTime");

        // Build the aggregation pipeline
        TypedAggregation<EventDocument.SessionInfo> aggregation = Aggregation.newAggregation(
                EventDocument.SessionInfo.class,
                matchEvent,
                unwindSessions,
                matchDateRange,
                replaceRoot,
                projectFields
        );

        // Execute the aggregation and return the results
        return reactiveMongoTemplate.aggregate(aggregation, "events", EventDocument.SessionInfo.class);
    }

}

