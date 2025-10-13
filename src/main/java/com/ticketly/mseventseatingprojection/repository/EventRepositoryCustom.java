package com.ticketly.mseventseatingprojection.repository;

import com.ticketly.mseventseatingprojection.dto.internal.EventAndSessionStatus;
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


    /**
     * Efficiently finds a single discount within an event by its ID.
     *
     * @param eventId    The ID of the parent event document.
     * @param discountId The ID of the discount to find.
     * @return A Mono emitting the found DiscountInfo or empty if not found.
     */
    Mono<EventDocument.DiscountInfo> findDiscountById(String eventId, String discountId);

    /**
     * Efficiently fetches only the event status and the status of a single, specific session.
     * @param eventId The ID of the event.
     * @param sessionId The ID of the session.
     * @return A Mono emitting the status information.
     */
    Mono<EventAndSessionStatus> findEventAndSessionStatus(String eventId, String sessionId, String organizationId);
}
