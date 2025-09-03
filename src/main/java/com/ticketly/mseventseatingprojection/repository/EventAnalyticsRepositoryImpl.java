package com.ticketly.mseventseatingprojection.repository;

import com.ticketly.mseventseatingprojection.dto.analytics.EventOverallStatsDTO;
import com.ticketly.mseventseatingprojection.dto.analytics.SessionStatusCountDTO;
import com.ticketly.mseventseatingprojection.dto.analytics.TierAnalyticsDTO;
import com.ticketly.mseventseatingprojection.model.EventDocument;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

@Repository
@RequiredArgsConstructor
public class EventAnalyticsRepositoryImpl implements EventAnalyticsRepository {

    private final ReactiveMongoTemplate reactiveMongoTemplate;

    private static final AggregationOperation UNIFY_SEATS_OPERATION = context -> Document.parse("""
        {
            "$project": {
                "allSeats": {
                    "$concatArrays": [
                        { "$ifNull": ["$sessions.layoutData.layout.blocks.seats", []] },
                        { "$ifNull": [
                            { "$reduce": {
                                "input": "$sessions.layoutData.layout.blocks.rows.seats",
                                "initialValue": [],
                                "in": { "$concatArrays": ["$$value", "$$this"] }
                            }},
                            []
                        ]}
                    ]
                }
            }
        }
        """);

    @Override
    public Mono<EventDocument> findEventWithCompleteSeatingData(String eventId) {
        Query query = new Query(Criteria.where("id").is(eventId));
        return reactiveMongoTemplate.findOne(query, EventDocument.class);
    }

    @Override
    public Mono<EventDocument> findSessionWithCompleteSeatingData(String eventId, String sessionId) {
        Query query = new Query(
                Criteria.where("id").is(eventId)
                        .and("sessions.id").is(sessionId)
        );
        return reactiveMongoTemplate.findOne(query, EventDocument.class);
    }

    @Override
    public Mono<EventOverallStatsDTO> getEventOverallStats(String eventId) {
        Aggregation aggregation = newAggregation(
                match(Criteria.where("_id").is(eventId)),
                unwind("sessions"),
                unwind("sessions.layoutData.layout.blocks"),
                // ++ USE THE NATIVE OPERATION HERE ++
                UNIFY_SEATS_OPERATION,
                unwind("allSeats"),
                replaceRoot("allSeats"),
                group()
                        .sum(
                                ConditionalOperators.when(Criteria.where("status").is("BOOKED"))
                                        .then(ConvertOperators.Convert.convertValue("$tier.price").to("decimal"))
                                        .otherwise(0)
                        ).as("totalRevenue")
                        .sum(
                                ConditionalOperators.when(Criteria.where("status").is("BOOKED")).then(1).otherwise(0)
                        ).as("totalTicketsSold")
                        .count().as("totalEventCapacity")
        );

        return reactiveMongoTemplate.aggregate(aggregation, "events", EventOverallStatsDTO.class)
                .next()
                .defaultIfEmpty(new EventOverallStatsDTO());
    }

    @Override
    public Flux<SessionStatusCountDTO> getSessionStatusCounts(String eventId) {
        Aggregation aggregation = newAggregation(
                match(Criteria.where("_id").is(eventId)),
                unwind("sessions"),
                group("sessions.status").count().as("count"),
                project("count").and("_id").as("status").andExclude("_id")
        );
        return reactiveMongoTemplate.aggregate(aggregation, "events", SessionStatusCountDTO.class);
    }

    @Override
    public Flux<TierAnalyticsDTO> getTierAnalytics(String eventId) {
        Aggregation aggregation = newAggregation(
                match(Criteria.where("_id").is(eventId)),
                unwind("sessions"),
                unwind("sessions.layoutData.layout.blocks"),
                UNIFY_SEATS_OPERATION,
                unwind("allSeats"),
                replaceRoot("allSeats"),
                match(Criteria.where("tier._id").ne(null)),
                group("tier._id")
                        .first("tier").as("tierData")
                        .count().as("tierCapacity")
                        .sum(
                                ConditionalOperators.when(Criteria.where("status").is("BOOKED")).then(1).otherwise(0)
                        ).as("ticketsSold")
                        .sum(
                                ConditionalOperators.when(Criteria.where("status").is("BOOKED"))
                                        .then(ConvertOperators.Convert.convertValue("$tier.price").to("decimal"))
                                        .otherwise(0)
                        ).as("totalRevenue"),
                project()
                        .and("tierData._id").as("tierId")
                        .and("tierData.name").as("tierName")
                        .and("tierData.color").as("tierColor")
                        .and("tierCapacity").as("tierCapacity")
                        .and("ticketsSold").as("ticketsSold")
                        .and("totalRevenue").as("totalRevenue")
                        .andExclude("_id")
        );

        return reactiveMongoTemplate.aggregate(aggregation, "events", TierAnalyticsDTO.class);
    }
}
