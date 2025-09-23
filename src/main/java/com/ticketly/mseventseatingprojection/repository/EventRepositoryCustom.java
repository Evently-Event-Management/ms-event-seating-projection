package com.ticketly.mseventseatingprojection.repository;

import com.ticketly.mseventseatingprojection.model.EventDocument;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.Map;


@Repository
public interface EventRepositoryCustom {
    /**
     * Atomically upserts a discount within an event's 'discounts' array.
     * If a discount with the same ID exists, it's updated.
     * If it does not exist, it's added to the array.
     *
     * @param eventId      The ID of the parent event document.
     * @param discountInfo The discount information to upsert.
     * @return A Mono emitting the number of documents modified.
     */
    Mono<Long> upsertDiscountInEvent(String eventId, EventDocument.DiscountInfo discountInfo);

    /**
     * Performs a targeted patch on a specific discount within an event's 'discounts' array.
     *
     * @param eventId The ID of the parent event document.
     * @param discountId The ID of the discount to patch.
     * @param fieldsToUpdate A map of field names (using dot notation for nested fields) and their new values.
     * @return A Mono emitting the number of documents modified.
     */
    Mono<Long> patchDiscountInEvent(String eventId, String discountId, Map<String, Object> fieldsToUpdate);
}
