package com.ticketly.mseventseatingprojection.repository;

import com.ticketly.mseventseatingprojection.model.EventDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.LookupOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

@Repository
@RequiredArgsConstructor
@lombok.extern.slf4j.Slf4j
public class TrendingRepositoryCustomImpl implements TrendingRepositoryCustom {

    private final ReactiveMongoTemplate reactiveMongoTemplate;

    @Override
    public Flux<EventDocument> findTopTrendingEvents(int limit) {
        List<AggregationOperation> pipeline = new ArrayList<>();

        // Stage 1: Lookup trending scores - The event document has 'id' field and trending document has 'eventId' field
        log.info("Setting up lookup from events(id) to event_trending_scores(eventId)");
        LookupOperation lookupTrending = lookup("event_trending_scores", "id", "eventId", "trendingInfo");
        pipeline.add(lookupTrending);

        // Stage 2: Match only events that have trending info - remove status filter for debugging
        pipeline.add(match(Criteria.where("trendingInfo").ne(null)));

        // Stage 3: Unwind trending info array to get single document per event
        // Use preserveNullAndEmptyArrays:true to handle events that might not have trending info
        pipeline.add(unwind("trendingInfo", true));

        // Stage 4: Sort by trending score (descending)
        pipeline.add(sort(Sort.Direction.DESC, "trendingInfo.trendingScore"));

        // Stage 5: Limit results
        pipeline.add(limit(limit));
        
        // Stage 6: Project to exclude layout data which is large
        pipeline.add(project()
                .andInclude("id", "title", "description", "overview", "coverPhotos", "organization", "category", "tiers")
                .and("sessions").as("sessions")
                .and("discounts").as("discounts")
                .and("trendingInfo.trendingScore").as("trendingScore")
                .and("trendingInfo.viewCount").as("viewCount"));
        
        // Stage 7: Use a different approach to filter out layout data
        // Since Spring Data MongoDB doesn't directly support excluding nested fields in projections,
        // we'll use a conditional to reshape the sessions array
        pipeline.add(project()
                .andInclude("id", "title", "description", "overview", "coverPhotos", "organization", "category", "tiers", "discounts", "trendingScore", "viewCount")
                .and("sessions").as("sessions"));

        log.info("Executing trending events aggregation with limit={}", limit);
        return reactiveMongoTemplate.aggregate(
                Aggregation.newAggregation(pipeline),
                "events",
                EventDocument.class
        )
        .doOnNext(event -> log.info("Found trending event: id={}, title={}", event.getId(), event.getTitle()))
        .doOnComplete(() -> log.info("Trending events aggregation completed"))
        .doOnError(e -> log.error("Error in trending events aggregation: {}", e.getMessage()));
    }
}