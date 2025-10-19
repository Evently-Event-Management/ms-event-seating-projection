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

        // Stage 1: Lookup trending scores
        log.info("Setting up lookup from events(id) to event_trending_scores(eventId)");
        LookupOperation lookupTrending = lookup("event_trending_scores", "_id", "eventId", "trendingInfo");
        pipeline.add(lookupTrending);

        // Stage 2: Unwind and Filter (THE FIX)
        // By *NOT* passing 'true', this stage now correctly deconstructs the array
        // AND automatically filters out any events that had no match 
        // (i.e., 'trendingInfo' was []). This removes "Neon Music".
        pipeline.add(unwind("trendingInfo"));

        // Stage 3: Sort by trending score (descending)
        // This will now work correctly as all remaining documents 
        // have a 'trendingInfo.trendingScore' field.
        pipeline.add(sort(Sort.Direction.DESC, "trendingInfo.trendingScore"));

        // Stage 4: Limit results
        pipeline.add(limit(limit));

        // Stage 5: Project to the final shape (This was your correct Stage 6)
        // The redundant Stage 7 is removed.
        pipeline.add(project()
                .andInclude("id", "title", "description", "overview", "coverPhotos", "organization", "category", "tiers")
                .and("sessions").as("sessions")
                .and("discounts").as("discounts")
                .and("trendingInfo.trendingScore").as("trendingScore")
                .and("trendingInfo.viewCount").as("viewCount"));

        log.info("Executing corrected trending events aggregation with limit={}", limit);
        return reactiveMongoTemplate.aggregate(
                        Aggregation.newAggregation(pipeline),
                        "events",
                        EventDocument.class // Assumes EventDocument has fields for trendingScore/viewCount
                )
                .doOnNext(event -> log.info("Found trending event: id={}, title={}",
                        event.getId(),
                        event.getTitle()))
                .doOnComplete(() -> log.info("Trending events aggregation completed"))
                .doOnError(e -> log.error("Error in trending events aggregation: {}", e.getMessage()));
    }
}