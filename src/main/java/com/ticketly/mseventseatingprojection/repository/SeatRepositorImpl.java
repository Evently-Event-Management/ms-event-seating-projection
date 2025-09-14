package com.ticketly.mseventseatingprojection.repository;


import com.mongodb.client.result.UpdateResult;
import com.ticketly.mseventseatingprojection.dto.internal.SeatValidationResponse;
import com.ticketly.mseventseatingprojection.model.EventDocument;
import com.ticketly.mseventseatingprojection.model.ReadModelSeatStatus;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
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

    /**
     * Validates the availability of the specified seats for a given event and session.
     *
     * @param eventId   The ID of the event.
     * @param sessionId The ID of the session.
     * @param seatIds   The list of seat IDs to validate.
     * @return A Mono emitting a SeatValidationResponse indicating which seats are unavailable.
     */
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

    /**
     * Finds and returns detailed information for the specified seats in a session.
     *
     * @param eventId   The ID of the event.
     * @param sessionId The ID of the session.
     * @param seatIds   The list of seat IDs to retrieve details for.
     * @return A Flux emitting EventDocument.SeatInfo for each requested seat.
     */
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

    @Override
    public Mono<Boolean> areAnySeatsBooked(String sessionId, List<String> seatIds) {
        // This query is optimized to stop searching as soon as it finds one match.
        Query query = Query.query(
                Criteria.where("sessions")
                        .elemMatch(
                                Criteria.where("id").is(sessionId)
                                        .and("layoutData.layout.blocks.rows.seats.id").in(seatIds)
                                        .and("layoutData.layout.blocks.rows.seats.status").is(ReadModelSeatStatus.BOOKED.toString())
                        )
        );
        return reactiveMongoTemplate.exists(query, EventDocument.class);
    }

    public Mono<Long> updateSeatStatuses(String sessionId, List<String> seatIds, ReadModelSeatStatus newStatus) {
        // 1. Query: Find the event document containing the session.
        Query query = Query.query(Criteria.where("sessions.id").is(sessionId));

        // 2. Update: Define the update operation with arrayFilters.
        Update update = new Update()
                .set("sessions.$[sess].layoutData.layout.blocks.$[].rows.$[].seats.$[seat].status", newStatus.toString())
                .set("sessions.$[sess].layoutData.layout.blocks.$[].seats.$[seat].status", newStatus.toString())
                // Define the 'sess' identifier for the session filter
                .filterArray("sess.id", sessionId)
                // Define the 'seat' identifier for the seat ID filter
                .filterArray(Criteria.where("seat.id").in(seatIds));

        // 3. Execute: Run the update and return the result.
        // Use updateMulti for safety, though we expect one document.
        return reactiveMongoTemplate.updateMulti(query, update, EventDocument.class)
                .map(UpdateResult::getModifiedCount);
    }
}
