package com.ticketly.mseventseatingprojection.repository;

import com.ticketly.mseventseatingprojection.dto.analytics.SessionSummaryDTO;
import com.ticketly.mseventseatingprojection.dto.analytics.TierSalesDTO;
import com.ticketly.mseventseatingprojection.dto.analytics.raw.EventOverallStatsDTO;
import com.ticketly.mseventseatingprojection.dto.analytics.raw.SessionStatusCountDTO;
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
    public Mono<EventDocument> findEventTitleById(String eventId) {
        Query query = new Query(Criteria.where("id").is(eventId));
        query.fields().include("title");
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
        AggregationOperation calculateStatsOperation = context -> Document.parse("""
                    {
                        "$group": {
                            "_id": null,
                            "totalRevenue": {
                                "$sum": {
                                    "$cond": [
                                        { "$eq": ["$status", "BOOKED"] },
                                        { "$toDecimal": "$tier.price" },
                                        0
                                    ]
                                }
                            },
                            "totalTicketsSold": {
                                "$sum": {
                                    "$cond": [
                                        { "$eq": ["$status", "BOOKED"] },
                                        1,
                                        0
                                    ]
                                }
                            },
                            "totalEventCapacity": { "$sum": 1 }
                        }
                    }
                """);

        AggregationOperation calculateDerivedMetrics = context -> Document.parse("""
                    {
                        "$addFields": {
                            "averageRevenuePerTicket": {
                                "$cond": [
                                    { "$gt": ["$totalTicketsSold", 0] },
                                    { "$divide": ["$totalRevenue", "$totalTicketsSold"] },
                                    0
                                ]
                            },
                            "overallSellOutPercentage": {
                                "$cond": [
                                    { "$gt": ["$totalEventCapacity", 0] },
                                    { "$multiply": [{ "$divide": ["$totalTicketsSold", "$totalEventCapacity"] }, 100] },
                                    0
                                ]
                            }
                        }
                    }
                """);

        Aggregation aggregation = newAggregation(
                match(Criteria.where("_id").is(eventId)),
                unwind("sessions"),
                unwind("sessions.layoutData.layout.blocks"),
                // ++ USE THE NATIVE OPERATION HERE ++
                UNIFY_SEATS_OPERATION,
                unwind("allSeats"),
                replaceRoot("allSeats"),
                calculateStatsOperation,
                calculateDerivedMetrics
        );

        return reactiveMongoTemplate.aggregate(aggregation, "events", EventOverallStatsDTO.class)
                .next()
                .defaultIfEmpty(new EventOverallStatsDTO());
    }

    @Override
    public Flux<SessionSummaryDTO> getAllSessionsAnalytics(String eventId) {
        // Define the complex stages using native JSON for clarity and correctness
        AggregationOperation unifySeatsOperation = context -> Document.parse("""
                    {
                        "$addFields": {
                            "unifiedSeats": {
                                "$reduce": {
                                    "input": "$sessions.layoutData.layout.blocks",
                                    "initialValue": [],
                                    "in": { "$concatArrays": [ "$$value", { "$ifNull": ["$$this.seats", []] }, { "$ifNull": [ { "$reduce": { "input": "$$this.rows.seats", "initialValue": [], "in": { "$concatArrays": ["$$value", "$$this"] }}}, [] ]} ] }
                                }
                            }
                        }
                    }
                """);

        AggregationOperation calculateStatsOperation = context -> Document.parse("""
                    {
                        "$addFields": {
                            "sessionCapacity": { "$size": "$unifiedSeats" },
                            "bookedSeats": {
                                "$filter": { "input": "$unifiedSeats", "as": "seat", "cond": { "$eq": ["$$seat.status", "BOOKED"] } }
                            }
                        }
                    }
                """);

        AggregationOperation calculateFinalMetricsOperation = context -> Document.parse("""
                    {
                        "$addFields": {
                            "ticketsSold": { "$size": "$bookedSeats" },
                            "sessionRevenue": { "$sum": { "$map": { "input": "$bookedSeats", "as": "seat", "in": { "$toDecimal": "$$seat.tier.price" } } } }
                        }
                    }
                """);

        // Add the sellOutPercentage directly as a Document stage
        AggregationOperation calculateSellOutPercentage = context -> Document.parse("""
                    {
                        "$addFields": {
                            "sellOutPercentage": {
                                "$cond": [
                                    { "$gt": ["$sessionCapacity", 0] },
                                    { "$multiply": [{ "$divide": ["$ticketsSold", "$sessionCapacity"] }, 100] },
                                    0
                                ]
                            }
                        }
                    }
                """);

        // Final projection without complex expressions
        AggregationOperation finalProjection = context -> Document.parse("""
                     {
                         "$project": {
                             "sessionId": "$sessions._id",
                             "eventId": "$_id",
                             "eventTitle": "$title",
                             "startTime": "$sessions.startTime",
                             "endTime": "$sessions.endTime",
                             "sessionStatus": "$sessions.status",
                             "sessionCapacity": 1,
                             "ticketsSold": 1,
                             "sessionRevenue": 1,
                             "sellOutPercentage": 1
                         }
                     }
                \s""");

        Aggregation aggregation = newAggregation(
                match(Criteria.where("_id").is(eventId)),
                unwind("sessions"),
                unifySeatsOperation,
                calculateStatsOperation,
                calculateFinalMetricsOperation,
                calculateSellOutPercentage,
                finalProjection
        );

        return reactiveMongoTemplate.aggregate(aggregation, "events", SessionSummaryDTO.class);
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
    public Flux<TierSalesDTO> getTierAnalytics(String eventId) {
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
                        .and(
                                ConditionalOperators.when(Criteria.where("tierCapacity").gt(0))
                                        .thenValueOf(
                                                ArithmeticOperators.Multiply.valueOf(
                                                        ArithmeticOperators.Divide.valueOf("ticketsSold").divideBy("tierCapacity")
                                                ).multiplyBy(100)
                                        )
                                        .otherwise(0)
                        ).as("percentageOfTotalSales")
                        .andExclude("_id")
        );

        return reactiveMongoTemplate.aggregate(aggregation, "events", TierSalesDTO.class);
    }


}