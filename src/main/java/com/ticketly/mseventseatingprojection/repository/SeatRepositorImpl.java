package com.ticketly.mseventseatingprojection.repository;


import com.ticketly.mseventseatingprojection.dto.internal.SeatValidationResponse;
import com.ticketly.mseventseatingprojection.model.EventDocument;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

@Repository
@RequiredArgsConstructor
public class SeatRepositorImpl implements SeatRepository {
    private final ReactiveMongoTemplate reactiveMongoTemplate;

    private final AggregationOperation UNIFY_SEATS_OPERATION = context -> Document.parse("""
            {
                "$project": {
                    "allSeats": {
                        "$reduce": {
                            "input": "$sessions.layoutData.layout.blocks",
                            "initialValue": [],
                            "in": {
                                "$concatArrays": [
                                    "$$value",
                                    { "$ifNull": ["$$this.seats", []] },
                                    { "$ifNull": [
                                        { "$reduce": {
                                            "input": "$$this.rows.seats",
                                            "initialValue": [],
                                            "in": { "$concatArrays": ["$$value", "$$this"] }
                                        }},
                                        []
                                    ]}
                                ]
                            }
                        }
                    }
                }
            }
            """);


    // A helper class to capture the simple string result
    private static class UnavailableSeat {
        public String unavailableSeatId;
    }

    @Override
    public Mono<SeatValidationResponse> validateSeatsAvailability(String eventId, String sessionId, List<String> seatIds) {
        // This query finds which of the requested seats are NOT available.
        Aggregation aggregation = newAggregation(
                match(Criteria.where("_id").is(eventId)),
                unwind("sessions"),
                match(Criteria.where("sessions._id").is(sessionId)),
                UNIFY_SEATS_OPERATION, // Re-use the unification logic
                unwind("allSeats"),
                match(
                        Criteria.where("allSeats._id").in(seatIds)
                                .and("allSeats.status").ne("AVAILABLE")
                ),
                project().and("allSeats._id").as("unavailableSeatId")
        );

        return reactiveMongoTemplate.aggregate(aggregation, "events", UnavailableSeat.class)
                .map(result -> result.unavailableSeatId)
                .collectList()
                .map(unavailableList -> SeatValidationResponse.builder()
                        .allAvailable(unavailableList.isEmpty())
                        .unavailableSeats(unavailableList)
                        .build());
    }

    @Override
    public Flux<EventDocument.SeatInfo> findSeatDetails(String eventId, String sessionId, List<String> seatIds) {
        // This query finds and returns the full documents for the requested seats.
        Aggregation aggregation = newAggregation(
                match(Criteria.where("_id").is(eventId)),
                unwind("sessions"),
                match(Criteria.where("sessions._id").is(sessionId)),
                UNIFY_SEATS_OPERATION,
                unwind("allSeats"),
                match(Criteria.where("allSeats._id").in(seatIds)),
                replaceRoot("allSeats")
        );

        return reactiveMongoTemplate.aggregate(aggregation, "events", EventDocument.SeatInfo.class);
    }
}
