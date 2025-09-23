package com.ticketly.mseventseatingprojection.repository;

import com.mongodb.client.result.UpdateResult;
import com.ticketly.mseventseatingprojection.model.EventDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.Map;

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
        fieldsToUpdate.forEach((key, value) -> {
            update.set("discounts.$[elem]." + key, value);
        });

        return reactiveMongoTemplate.updateFirst(query, update, EventDocument.class)
                .map(UpdateResult::getModifiedCount);
    }
}
