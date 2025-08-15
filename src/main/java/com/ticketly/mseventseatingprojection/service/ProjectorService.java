package com.ticketly.mseventseatingprojection.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketly.mseventseatingprojection.dto.CategoryChangePayload;
import com.ticketly.mseventseatingprojection.dto.OrganizationChangePayload;
import com.ticketly.mseventseatingprojection.dto.SessionSeatingMapDTO;
import com.ticketly.mseventseatingprojection.model.CategoryDocument;
import com.ticketly.mseventseatingprojection.model.EventDocument;
import com.ticketly.mseventseatingprojection.model.OrganizationDocument;
import com.ticketly.mseventseatingprojection.repository.CategoryRepository;
import com.ticketly.mseventseatingprojection.repository.EventRepository;
import com.ticketly.mseventseatingprojection.repository.OrganizationRepository;
import com.ticketly.mseventseatingprojection.service.mapper.EventProjectionMapper;
import com.ticketly.mseventseatingprojection.service.mapper.SessionSeatingMapper;
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
    private final OrganizationRepository organizationRepository; // ✅ Added repository
    private final CategoryRepository categoryRepository;     // ✅ Added repository
    private final ObjectMapper objectMapper;
    private final EventProjectionMapper eventProjectionMapper;
    private final SessionSeatingMapper sessionSeatingMapper;
    private final S3UrlGenerator s3UrlGenerator;

    public Mono<Void> projectFullEvent(UUID eventId) {
        log.info("Projecting full event for ID: {}", eventId);
        return eventProjectionClient.getEventProjectionData(eventId)
                .map(eventProjectionMapper::fromProjection) // clear intent: projection mapping
                .flatMap(eventRepository::save)
                .then();
    }

    public Mono<Void> deleteEvent(UUID eventId) {
        log.info("Deleting event {} from read model.", eventId);
        return eventRepository.deleteById(eventId.toString());
    }

    public Mono<Void> projectSessionUpdate(UUID eventId, UUID sessionId) {
        log.info("Projecting session update for event ID: {} and session ID: {}", eventId, sessionId);
        return eventProjectionClient.getSessionProjectionData(sessionId)
                .map(eventProjectionMapper::fromSession) // clear intent: projection session mapping
                .flatMap(sessionInfo -> eventRepository.updateSessionInEvent(eventId.toString(), sessionId.toString(), sessionInfo))
                .then();
    }

    public Mono<Void> projectSeatingMapPatch(UUID eventId, UUID sessionId, String layoutJson) {
        log.info("Projecting seating map update for event ID: {} and session ID: {}", eventId, sessionId);

        return eventRepository.findById(eventId.toString())
                .flatMap(eventDocument -> {
                    try {
                        SessionSeatingMapDTO seatingMapDto =
                                objectMapper.readValue(layoutJson, SessionSeatingMapDTO.class);

                        Map<String, EventDocument.TierInfo> tierInfoMap = eventDocument.getTiers().stream()
                                .collect(Collectors.toMap(EventDocument.TierInfo::getId, Function.identity()));

                        EventDocument.SessionSeatingMapInfo seatingMapInfo =
                                sessionSeatingMapper.fromSessionMap(seatingMapDto, tierInfoMap);

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

    public Mono<Void> projectOrganizationChange(OrganizationChangePayload orgChange) {
        log.info("Projecting organization change for ID: {}", orgChange.getId());

        // 1. Upsert the document in the 'organizations' collection
        OrganizationDocument orgDoc = OrganizationDocument.builder()
                .id(orgChange.getId().toString()).name(orgChange.getName())
                .logoUrl(s3UrlGenerator.generatePublicUrl(orgChange.getLogoUrl())).website(orgChange.getWebsite()).build();

        Mono<OrganizationDocument> saveOrgMono = organizationRepository.save(orgDoc)
                .doOnSuccess(savedDoc -> log.info("Upserted organization document with ID: {}", savedDoc.getId()));

        // 2. Trigger a bulk update for all events that embed this organization's info
        EventDocument.OrganizationInfo embeddedInfo = EventDocument.OrganizationInfo.builder()
                .id(orgChange.getId().toString()).name(orgChange.getName()).logoUrl(s3UrlGenerator.generatePublicUrl(orgChange.getLogoUrl())).build();

        Mono<Long> updateEventsMono = eventRepository.updateOrganizationInfoInEvents(orgChange.getId().toString(), embeddedInfo)
                .doOnSuccess(count -> log.info("Updated embedded organization info for {} events.", count));

        // Combine both operations to run in parallel and complete when both are done
        return Mono.zip(saveOrgMono, updateEventsMono).then();
    }

    public Mono<Void> deleteOrganization(String orgId) {
        log.info("Deleting organization {} from read model.", orgId);
        // In a real system, you might also trigger a process to handle events of a deleted organization
        return organizationRepository.deleteById(orgId);
    }

    // ✅ NEW: Logic for handling category changes, moved from the consumer
    public Mono<Void> projectCategoryChange(CategoryChangePayload catChange) {
        log.info("Projecting category change for ID: {}", catChange.getId());

        Mono<CategoryDocument> parentCategoryMono = catChange.getParentId() != null
                ? categoryRepository.findById(catChange.getParentId().toString())
                : Mono.empty();

        return parentCategoryMono.defaultIfEmpty(CategoryDocument.builder().build())
                .flatMap(parentCat -> {
                    CategoryDocument catDoc = CategoryDocument.builder()
                            .id(catChange.getId().toString())
                            .name(catChange.getName())
                            .parentName(parentCat.getName())
                            .build();

                    // 1. Upsert the document in the 'categories' collection
                    return categoryRepository.save(catDoc)
                            .doOnSuccess(savedDoc -> log.info("Upserted category document with ID: {}", savedDoc.getId()))
                            .then(Mono.just(catDoc)); // Pass the document down the chain
                })
                .flatMap(catDoc -> {
                    // 2. Trigger a bulk update for all events that embed this category's info
                    EventDocument.CategoryInfo embeddedInfo = EventDocument.CategoryInfo.builder()
                            .id(catDoc.getId())
                            .name(catDoc.getName())
                            .parentName(catDoc.getParentName())
                            .build();
                    return eventRepository.updateCategoryInfoInEvents(catDoc.getId(), embeddedInfo);
                })
                .doOnSuccess(count -> log.info("Updated embedded category info for {} events.", count))
                .then();
    }

    public Mono<Void> deleteCategory(String catId) {
        log.info("Deleting category {} from read model.", catId);
        // In a real system, you might trigger a process to re-categorize events
        return categoryRepository.deleteById(catId);
    }


    public Mono<Void> projectCoverPhotoAdded(UUID eventId, String photoKey) {
        log.info("Projecting cover photo addition for event ID: {}", eventId);
        // Transform the S3 key into a full, public URL
        String publicUrl = s3UrlGenerator.generatePublicUrl(photoKey);
        return eventRepository.addCoverPhotoToEvent(eventId.toString(), publicUrl).then();
    }

    /**
     * Handles removing a cover photo from an event document.
     */
    public Mono<Void> projectCoverPhotoRemoved(UUID eventId, String photoKey) {
        log.info("Projecting cover photo removal for event ID: {}", eventId);
        String publicUrl = s3UrlGenerator.generatePublicUrl(photoKey);
        return eventRepository.removeCoverPhotoFromEvent(eventId.toString(), publicUrl).then();
    }
}
