package com.ticketly.mseventseatingprojection.repository;

import com.ticketly.mseventseatingprojection.model.EventDocument;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface EventRepository extends ReactiveMongoRepository<EventDocument, String> {

    /**
     * Finds the event document containing a session by session ID.
     *
     * @param sessionId The session ID to search for.
     * @return Mono emitting the EventDocument containing the session.
     */
    @Query("{ 'sessions.id': ?0 }")
    Mono<EventDocument> findEventBySessionId(String sessionId);

    /**
     * Efficiently finds a single session within an event by its ID.
     * Uses a projection to return only the matching session object, EXCLUDING
     * the large 'layoutData' field for maximum efficiency.
     *
     * @param sessionId The ID of the session to find.
     * @return A Mono emitting the found SessionInfo or empty if not found.
     */
    @Query(value = "{ 'sessions.id': ?0 }", fields = "{ 'sessions.$': 1, 'sessions.$.layoutData': 0 }")
    Mono<EventDocument> findSessionById(String sessionId);

    /**
     * Performs a targeted update on a single session within an event document.
     * The '$' is a positional operator that updates the first element in the 'sessions'
     * array that matches the query condition.
     *
     * @param eventId     The ID of the parent event document.
     * @param sessionId   The ID of the session to identify for the update.
     * @param sessionInfo The complete, updated SessionInfo object to replace the old one.
     * @return A Mono emitting the number of documents modified.
     */
    @Query("{ '_id': ?0, 'sessions.id': ?1 }")
    @Update("{ '$set': { 'sessions.$': ?2 } }")
    Mono<Long> updateSessionInEvent(String eventId, String sessionId, EventDocument.SessionInfo sessionInfo);

    /**
     * Performs a targeted update on the seating map of a single session.
     *
     * @param eventId        The ID of the parent event document.
     * @param sessionId      The ID of the session to update.
     * @param seatingMapInfo The new, complete SessionSeatingMapInfo object.
     * @return A Mono emitting the number of documents modified.
     */
    @Query("{ '_id': ?0, 'sessions.id': ?1 }")
    @Update("{ '$set': { 'sessions.$.layoutData': ?2 } }")
    Mono<Long> updateSeatingMapInSession(String eventId, String sessionId, EventDocument.SessionSeatingMapInfo seatingMapInfo);

    /**
     * Updates the organization information in all events that belong to a specific organization.
     * This is useful when an organization changes its details and we need to propagate those changes
     * to all associated events.
     *
     * @param organizationId   The ID of the organization whose events are to be updated.
     *                         This should match the 'organization.id' field in the event documents.
     * @param organizationInfo The new organization information to set in the events.
     * @return A Mono emitting the number of documents modified. This will be 0 if no events were found
     */
    @Query("{ 'organization.id': ?0 }")
    @Update("{ '$set': { 'organization': ?1 } }")
    Mono<Long> updateOrganizationInfoInEvents(String organizationId, EventDocument.OrganizationInfo organizationInfo);

    /**
     * Updates the category information in all events that belong to a specific category.
     * This is useful when a category changes its details and we need to propagate those changes
     * to all associated events.
     *
     * @param categoryId   The ID of the category whose events are to be updated.
     * @param categoryInfo The new category information to set in the events.
     * @return A Mono emitting the number of documents modified. This will be 0 if
     */
    @Query("{ 'category.id': ?0 }")
    @Update("{ '$set': { 'category': ?1 } }")
    Mono<Long> updateCategoryInfoInEvents(String categoryId, EventDocument.CategoryInfo categoryInfo);

    /**
     * Atomically adds a new photo URL to the coverPhotos array of an event.
     * Uses $addToSet to ensure no duplicate URLs are added.
     *
     * @param eventId  The ID of the event document to update.
     * @param photoUrl The public URL of the photo to add.
     * @return A Mono emitting the number of documents modified.
     */
    @Query("{ '_id': ?0 }")
    @Update("{ '$addToSet': { 'coverPhotos': ?1 } }")
    Mono<Long> addCoverPhotoToEvent(String eventId, String photoUrl);

    /**
     * Atomically removes a specific photo URL from the coverPhotos array of an event.
     *
     * @param eventId  The ID of the event document to update.
     * @param photoUrl The public URL of the photo to remove.
     * @return A Mono emitting the number of documents modified.
     */
    @Query("{ '_id': ?0 }")
    @Update("{ '$pull': { 'coverPhotos': ?1 } }")
    Mono<Long> removeCoverPhotoFromEvent(String eventId, String photoUrl);


    /**
     * Quickly checks the ownership of an event by fetching only the organization's userId.
     * This is highly efficient as it avoids loading the entire event document.
     *
     * @param eventId The ID of the event to check.
     * @return A Mono emitting the EventDocument containing ONLY the _id and organization.userId fields.
     */
    @Query(value = "{ '_id': ?0 }", fields = "{ 'organization.userId': 1 }")
    Mono<EventDocument> findOwnerIdByEventId(String eventId);


    /**
     * Quickly fetches an event's title by its ID using a projection.
     * Spring Data will automatically implement this method.
     *
     * @param eventId The ID of the event to find.
     * @return A Mono emitting the EventDocument containing ONLY the _id and title fields.
     */
    @Query(value = "{ 'id': ?0 }", fields = "{ 'title': 1 }")
    Mono<EventDocument> findEventTitleById(String eventId);

    /**
     * Atomically removes a specific discount sub-document from the discounts array
     * based on its ID.
     *
     * @param eventId    The ID of the parent event document.
     * @param discountId The ID of the discount to remove from the array.
     * @return A Mono emitting the number of documents modified.
     */
    @Query("{ '_id': ?0 }")
    @Update("{ '$pull': { 'discounts': { 'id': ?1 } } }")
    Mono<Long> removeDiscountFromEvent(String eventId, String discountId);

    /**
     * Finds an event's ID containing a specific discount by discount ID.
     * Uses a projection to return ONLY the event's top-level ID for maximum efficiency.
     *
     * @param discountId The ID of the discount to search for.
     * @return Mono emitting the EventDocument containing only its ID.
     */
    @Query(value = "{ 'discounts.id': ?0 }", fields = "{ '_id': 1 }")
    Mono<EventDocument> findEventIdByDiscountId(String discountId);
}
