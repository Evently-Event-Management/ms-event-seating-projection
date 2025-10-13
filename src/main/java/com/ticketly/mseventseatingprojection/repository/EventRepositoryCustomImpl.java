package com.ticketly.mseventseatingprojection.repository;

import com.mongodb.client.result.UpdateResult;
import com.ticketly.mseventseatingprojection.dto.internal.EventAndSessionStatus;
import com.ticketly.mseventseatingprojection.model.EventDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.ArrayOperators;
import org.springframework.data.mongodb.core.aggregation.ComparisonOperators;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.Map;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

@Repository
@RequiredArgsConstructor
public class EventRepositoryCustomImpl implements EventRepositoryCustom {
    private final ReactiveMongoTemplate reactiveMongoTemplate;


    @Override
    public Mono<Long> upsertDiscountInEvent(String eventId, EventDocument.DiscountInfo discountInfo) {
        // Step 1: Try to update an existing discount in the array.
        // We use arrayFilters to target a specific element in the array by its ID.
        Query query = new Query(Criteria.where("_id").is(eventId));
        Update updateExisting = new Update()
                .set("discounts.$[elem]", discountInfo)
                .filterArray(Criteria.where("elem._id").is(discountInfo.getId()));

        return reactiveMongoTemplate.updateFirst(query, updateExisting, EventDocument.class)
                .flatMap(updateResult -> {
                    // Step 2: If nothing was modified, the discount didn't exist. Add it to the set.
                    if (updateResult.getModifiedCount() == 0) {
                        Update addNew = new Update().addToSet("discounts", discountInfo);
                        return reactiveMongoTemplate.updateFirst(query, addNew, EventDocument.class)
                                .map(UpdateResult::getModifiedCount);
                    }
                    // If the update succeeded, just return the count.
                    return Mono.just(updateResult.getModifiedCount());
                });
    }

    @Override
    public Mono<Long> patchDiscountInEvent(String eventId, String discountId, Map<String, Object> fieldsToUpdate) {
        Query query = new Query(Criteria.where("_id").is(eventId));

        Update update = new Update();
        update.filterArray(Criteria.where("elem._id").is(discountId));

        // Dynamically build the $set operation
        fieldsToUpdate.forEach((key, value) -> update.set("discounts.$[elem]." + key, value));

        return reactiveMongoTemplate.updateFirst(query, update, EventDocument.class)
                .map(UpdateResult::getModifiedCount);
    }

    @Override
    public Mono<EventDocument.DiscountInfo> findDiscountById(String eventId, String discountId) {
        // Build the aggregation pipeline to extract the specific discount
        Aggregation aggregation = Aggregation.newAggregation(
                // Stage 1: Find the correct parent event document
                match(Criteria.where("_id").is(eventId)),

                // Stage 2: Deconstruct the 'discounts' array into a stream of documents
                unwind("$discounts"),

                // Stage 3: Filter the stream to find the specific discount by its ID
                match(Criteria.where("discounts._id").is(discountId)),

                // Stage 4: Promote the found discount object to be the root of the result
                Aggregation.replaceRoot("$discounts")
        );

        // Execute the aggregation, expecting a DiscountInfo object back, and return the single result or an empty Mono
        return reactiveMongoTemplate.aggregate(aggregation, "events", EventDocument.DiscountInfo.class)
                .singleOrEmpty();
    }


    @Override
    public Mono<EventAndSessionStatus> findEventAndSessionStatus(String eventId, String sessionId, String organizationId) {
        Aggregation aggregation = Aggregation.newAggregation(
                // 1. Match the parent event
                match(Criteria.where("_id").is(eventId)
                        .and("organization._id").is(organizationId)),
                // 2. Project just the event status and the specific session we need
                project("status")
                        .and(ArrayOperators.Filter.filter("sessions")
                                .as("session")
                                .by(ComparisonOperators.Eq.valueOf("$$session._id").equalToValue(sessionId)))
                        .as("matchedSession"),
                // 3. Unwind the single-element array
                unwind("matchedSession"),
                // 4. Project into the final shape
                project()
                        .and("status").as("eventStatus")
                        .and("matchedSession.status").as("sessionStatus")
        );
        return reactiveMongoTemplate.aggregate(aggregation, "events", EventAndSessionStatus.class).singleOrEmpty();
    }
}
