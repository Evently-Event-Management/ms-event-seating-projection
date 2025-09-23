package com.ticketly.mseventseatingprojection.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketly.mseventseatingprojection.dto.DiscountMetadataChangePayload;
import com.ticketly.mseventseatingprojection.dto.OrganizationChangePayload;
import com.ticketly.mseventseatingprojection.model.CategoryDocument;
import com.ticketly.mseventseatingprojection.model.EventDocument;
import com.ticketly.mseventseatingprojection.model.OrganizationDocument;
import com.ticketly.mseventseatingprojection.repository.*;
import com.ticketly.mseventseatingprojection.service.mapper.EventProjectionMapper;
import com.ticketly.mseventseatingprojection.service.mapper.SeatingMapMapper;
import dto.SessionSeatingMapDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectorService {

    private final EventProjectionClient eventProjectionClient;
    private final EventRepository eventRepository;
    private final EventRepositoryCustom eventRepositoryCustom;
    private final OrganizationRepository organizationRepository;
    private final CategoryRepository categoryRepository;
    private final ObjectMapper objectMapper;
    private final EventProjectionMapper eventProjectionMapper;
    private final SeatingMapMapper seatingMapMapper;
    private final S3UrlGenerator s3UrlGenerator;


    /**
     * Projects the full event data into the read model for the given event ID.
     *
     * @param eventId The UUID of the event.
     * @return Mono signaling completion.
     */
    public Mono<Void> projectFullEvent(UUID eventId) {
        log.info("Projecting full event for ID: {}", eventId);
        return eventProjectionClient.getEventProjectionData(eventId)
                .map(eventProjectionMapper::fromProjection) // clear intent: projection mapping
                .flatMap(eventRepository::save)
                .then();
    }

    /**
     * Deletes the event from the read model for the given event ID.
     *
     * @param eventId The UUID of the event.
     * @return Mono signaling completion.
     */
    public Mono<Void> deleteEvent(UUID eventId) {
        log.info("Deleting event {} from read model.", eventId);
        return eventRepository.deleteById(eventId.toString());
    }

    /**
     * Projects a session update for the given event and session IDs.
     *
     * @param eventId   The UUID of the event.
     * @param sessionId The UUID of the session.
     * @return Mono signaling completion.
     */
    public Mono<Void> projectSessionUpdate(UUID eventId, UUID sessionId) {
        log.info("Projecting session update for event ID: {} and session ID: {}", eventId, sessionId);
        return eventProjectionClient.getSessionProjectionData(sessionId)
                .map(eventProjectionMapper::fromSession) // clear intent: projection session mapping
                .flatMap(sessionInfo -> eventRepository.updateSessionInEvent(eventId.toString(), sessionId.toString(), sessionInfo))
                .then();
    }

    /**
     * Projects a patch to the seating map for a session.
     *
     * @param eventId    The UUID of the event.
     * @param sessionId  The UUID of the session.
     * @param layoutJson The JSON string representing the seating map layout.
     * @return Mono signaling completion.
     */
    public Mono<Void> projectSeatingMapPatch(UUID eventId, UUID sessionId, String layoutJson) {
        log.info("Projecting seating map patch for event ID: {} and session ID: {}", eventId, sessionId);

        return eventRepository.findById(eventId.toString())
                .flatMap(eventDocument -> {
                    try {
                        SessionSeatingMapDTO seatingMapDto =
                                objectMapper.readValue(layoutJson, SessionSeatingMapDTO.class);

                        Map<String, EventDocument.TierInfo> tierInfoMap = eventDocument.getTiers().stream()
                                .collect(Collectors.toMap(EventDocument.TierInfo::getId, Function.identity()));

                        EventDocument.SessionSeatingMapInfo seatingMapInfo =
                                seatingMapMapper.fromSessionMap(seatingMapDto, tierInfoMap);

                        return eventRepository.updateSeatingMapInSession(
                                eventId.toString(), sessionId.toString(), seatingMapInfo
                        );
                    } catch (Exception e) {
                        log.error("Failed to process seating map update for session {}", sessionId, e);
                        return Mono.empty();
                    }
                })
                .then();
    }

    /**
     * Projects an organization change into the read model and updates embedded organization info in events.
     *
     * @param orgChange The organization change payload.
     * @return Mono signaling completion.
     */
    public Mono<Void> projectOrganizationChange(OrganizationChangePayload orgChange) {
        log.info("Projecting organization change for ID: {}", orgChange.getId());

        // 1. Upsert the document in the 'organizations' collection
        OrganizationDocument orgDoc = OrganizationDocument.builder()
                .id(orgChange.getId().toString()).name(orgChange.getName()).userId(orgChange.getUserId())
                .logoUrl(s3UrlGenerator.generatePublicUrl(orgChange.getLogoUrl())).website(orgChange.getWebsite()).build();

        Mono<OrganizationDocument> saveOrgMono = organizationRepository.save(orgDoc)
                .doOnSuccess(savedDoc -> log.info("Upserted organization document with ID: {}", savedDoc.getId()));

        // 2. Trigger a bulk update for all events that embed this organization's info
        EventDocument.OrganizationInfo embeddedInfo = EventDocument.OrganizationInfo.builder()
                .id(orgChange.getId().toString()).name(orgChange.getName()).logoUrl(s3UrlGenerator.generatePublicUrl(orgChange.getLogoUrl())).userId(orgChange.getUserId()).build();

        Mono<Long> updateEventsMono = eventRepository.updateOrganizationInfoInEvents(orgChange.getId().toString(), embeddedInfo)
                .doOnSuccess(count -> log.info("Updated embedded organization info for {} events.", count));

        // Combine both operations to run in parallel and complete when both are done
        return Mono.zip(saveOrgMono, updateEventsMono).then();
    }

    /**
     * Deletes the organization from the read model for the given organization ID.
     *
     * @param orgId The ID of the organization.
     * @return Mono signaling completion.
     */
    public Mono<Void> deleteOrganization(String orgId) {
        log.info("Deleting organization {} from read model.", orgId);
        // In a real system, you might also trigger a process to handle events of a deleted organization
        return organizationRepository.deleteById(orgId);
    }

    /**
     * Projects a category change into the read model and updates embedded category info in events.
     *
     * @param categoryId The UUID of the category.
     * @return Mono signaling completion.
     */
    public Mono<Void> projectCategoryChange(UUID categoryId) {
        log.info("Projecting category change for ID: {}", categoryId);
        return eventProjectionClient.getCategoryProjectionData(categoryId)
                .flatMap(catDto -> {
                    CategoryDocument catDoc = CategoryDocument.builder()
                            .id(catDto.getId().toString())
                            .name(catDto.getName())
                            .parentId(catDto.getParentId() != null ? catDto.getParentId().toString() : null)
                            .parentName(catDto.getParentName())
                            .build();

                    Mono<CategoryDocument> saveCatMono = categoryRepository.save(catDoc)
                            .doOnSuccess(savedDoc -> log.info("Upserted category document with ID: {}", savedDoc.getId()));

                    EventDocument.CategoryInfo embeddedInfo = EventDocument.CategoryInfo.builder()
                            .id(catDoc.getId())
                            .name(catDoc.getName())
                            .parentName(catDoc.getParentName())
                            .build();

                    Mono<Long> updateEventsMono = eventRepository.updateCategoryInfoInEvents(catDoc.getId(), embeddedInfo)
                            .doOnSuccess(count -> log.info("Updated embedded category info for {} events.", count));

                    return Mono.zip(saveCatMono, updateEventsMono);
                })
                .then();
    }

    /**
     * Deletes the category from the read model for the given category ID.
     *
     * @param catId The ID of the category.
     * @return Mono signaling completion.
     */
    public Mono<Void> deleteCategory(String catId) {
        log.info("Deleting category {} from read model.", catId);
        // In a real system, you might trigger a process to re-categorize events
        return categoryRepository.deleteById(catId);
    }


    /**
     * Projects the addition of a cover photo to an event.
     *
     * @param eventId  The UUID of the event.
     * @param photoKey The S3 key of the photo.
     * @return Mono signaling completion.
     */
    public Mono<Void> projectCoverPhotoAdded(UUID eventId, String photoKey) {
        log.info("Projecting cover photo addition for event ID: {}", eventId);
        // Transform the S3 key into a full, public URL
        String publicUrl = s3UrlGenerator.generatePublicUrl(photoKey);
        return eventRepository.addCoverPhotoToEvent(eventId.toString(), publicUrl).then();
    }

    /**
     * Handles removing a cover photo from an event document.
     *
     * @param eventId  The UUID of the event.
     * @param photoKey The S3 key of the photo.
     * @return Mono signaling completion.
     */
    public Mono<Void> projectCoverPhotoRemoved(UUID eventId, String photoKey) {
        log.info("Projecting cover photo removal for event ID: {}", eventId);
        String publicUrl = s3UrlGenerator.generatePublicUrl(photoKey);
        return eventRepository.removeCoverPhotoFromEvent(eventId.toString(), publicUrl).then();
    }

    /**
     * Projects a discount change by fetching its latest state and upserting it
     * into the parent event's embedded discount list.
     *
     * @param eventId    The parent event's ID.
     * @param discountId The ID of the discount that changed.
     * @return Mono signaling completion.
     */
    public Mono<Void> projectDiscountChange(UUID eventId, UUID discountId) {
        log.info("Projecting discount change for event ID: {} and discount ID: {}", eventId, discountId);
        // "Signal and Fetch" pattern: Debezium is the signal, this client call is the fetch.
        return eventProjectionClient.getDiscountProjectionData(discountId)
                .map(eventProjectionMapper::fromDiscount) // Map the DTO to the embedded document
                .flatMap(discountInfo ->
                        eventRepositoryCustom.upsertDiscountInEvent(eventId.toString(), discountInfo)
                )
                .then();
    }

    /**
     * Projects the deletion of a discount by removing it from the parent
     * event's embedded discount list.
     *
     * @param eventId    The parent event's ID.
     * @param discountId The ID of the discount to remove.
     * @return Mono signaling completion.
     */
    public Mono<Void> projectDiscountDeletion(UUID eventId, UUID discountId) {
        log.info("Projecting discount deletion for event ID: {} and discount ID: {}", eventId, discountId);
        return eventRepository.removeDiscountFromEvent(eventId.toString(), discountId.toString()).then();
    }

    /**
     * Applies a partial update (patch) to an existing discount within an event document.
     *
     * @param payload The Debezium payload containing the changed discount metadata.
     * @return Mono signaling completion.
     */
    public Mono<Void> patchDiscount(DiscountMetadataChangePayload payload) {
        String eventId = payload.getEventId().toString();
        String discountId = payload.getId().toString();
        log.info("Patching discount {} in event {}", discountId, eventId);

        // Convert the payload to a map of fields to be updated
        Map<String, Object> fieldsToUpdate = objectMapper.convertValue(payload, new com.fasterxml.jackson.core.type.TypeReference<>() {});

        // Remove keys that are not part of the DiscountInfo sub-document or used for identification
        fieldsToUpdate.remove("id");
        fieldsToUpdate.remove("eventId");

        // Convert OffsetDateTime to Instant for MongoDB compatibility
        if (payload.getActiveFrom() != null) {
            fieldsToUpdate.put("activeFrom", payload.getActiveFrom().toInstant());
        }
        if (payload.getExpiresAt() != null) {
            fieldsToUpdate.put("expiresAt", payload.getExpiresAt().toInstant());
        }

        return eventRepositoryCustom.patchDiscountInEvent(eventId, discountId, fieldsToUpdate).then();
    }
}
