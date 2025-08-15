package com.ticketly.mseventseatingprojection.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketly.mseventseatingprojection.dto.*;
import com.ticketly.mseventseatingprojection.model.CategoryDocument;
import com.ticketly.mseventseatingprojection.model.EventDocument;
import com.ticketly.mseventseatingprojection.model.OrganizationDocument;
import com.ticketly.mseventseatingprojection.repository.CategoryRepository;
import com.ticketly.mseventseatingprojection.repository.EventRepository;
import com.ticketly.mseventseatingprojection.repository.OrganizationRepository;
import com.ticketly.mseventseatingprojection.service.ProjectorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DebeziumEventConsumer {

    private final ProjectorService projectorService;
    private final EventRepository eventReadRepository;
    private final ObjectMapper objectMapper;
    private final OrganizationRepository organizationRepository;
    private final EventRepository eventRepository;
    private final CategoryRepository categoryRepository;

    // ✅ A single listener for all Debezium topics
    @KafkaListener(topics = {
            "dbz.ticketly.public.events",
            "dbz.ticketly.public.event_sessions",
            "dbz.ticketly.public.session_seating_maps",
            "dbz.ticketly.public.organizations",
            "dbz.ticketly.public.categories"
    })
    public void onDebeziumEvent(String payload, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.debug("Received message on topic: {}", topic);
        try {
            // Use the topic name to decide which logic to execute
            if (topic.endsWith(".events")) {
                processEventChange(payload);
            } else if (topic.endsWith(".event_sessions")) {
                processSessionChange(payload);
            } else if (topic.endsWith(".session_seating_maps")) {
                processSeatingMapChange(payload);
            } else if (topic.endsWith(".organizations")) {
                processOrganizationChange(payload);
            } else if (topic.endsWith(".categories")) {
                processCategoryChange(payload);
            }
        } catch (Exception e) {
            log.error("Error processing Debezium event from topic {}: {}", topic, payload, e);
        }
    }

    private void processEventChange(String payload) throws Exception {
        JsonNode message = objectMapper.readTree(payload).path("payload");
        String operation = message.path("op").asText();

        if ("d".equals(operation)) {
            UUID eventId = UUID.fromString(message.path("before").path("id").asText());
            projectorService.deleteEvent(eventId).subscribe();
            return;
        }

        EventChangePayload eventChange = objectMapper.treeToValue(message.path("after"), EventChangePayload.class);
        if ("APPROVED".equals(eventChange.getStatus())) {
            projectorService.projectFullEvent(eventChange.getId()).subscribe();
        } else {
            projectorService.deleteEvent(eventChange.getId()).subscribe();
        }
    }

    private void processSessionChange(String payload) throws Exception {
        JsonNode message = objectMapper.readTree(payload).path("payload");
        String operation = message.path("op").asText();
        if ("d".equals(operation)) return;

        SessionChangePayload sessionChange = objectMapper.treeToValue(message.path("after"), SessionChangePayload.class);
        eventReadRepository.existsById(sessionChange.getEventId().toString())
                .filter(exists -> exists)
                .flatMap(exists -> projectorService.projectSessionUpdate(sessionChange.getEventId(), sessionChange.getId()))
                .subscribe();
    }

    private void processSeatingMapChange(String payload) throws Exception {
        JsonNode message = objectMapper.readTree(payload).path("payload");
        String operation = message.path("op").asText();
        if ("c".equals(operation) || "d".equals(operation)) return;

        SeatingMapChangePayload mapChange = objectMapper.treeToValue(message.path("after"), SeatingMapChangePayload.class);
        eventReadRepository.findEventBySessionId(mapChange.getSessionId().toString())
                .flatMap(eventDocument -> {
                    EventDocument.SessionInfo session = eventDocument.getSessions().stream()
                            .filter(s -> s.getId().equals(mapChange.getSessionId().toString()))
                            .findFirst().orElse(null);

                    if (session == null) return Mono.empty();

                    if ("ON_SALE".equals(session.getStatus())) {
                        return projectorService.projectSeatingMapPatch(UUID.fromString(eventDocument.getId()), mapChange.getSessionId(), mapChange.getLayoutData());
                    } else {
                        return projectorService.projectSessionUpdate(UUID.fromString(eventDocument.getId()), mapChange.getSessionId());
                    }
                }).subscribe();
    }

    private void processOrganizationChange(String payload) throws Exception {
        JsonNode message = objectMapper.readTree(payload).path("payload");
        String operation = message.path("op").asText();

        if ("d".equals(operation)) { // Handle delete
            String orgId = message.path("before").path("id").asText();
            organizationRepository.deleteById(orgId).subscribe();
            // You might also want to delete or anonymize events under this org
            return;
        }

        OrganizationChangePayload orgChange = objectMapper.treeToValue(message.path("after"), OrganizationChangePayload.class);

        // 1. Upsert the document in the 'organizations' collection
        OrganizationDocument orgDoc = OrganizationDocument.builder()
                .id(orgChange.getId().toString()).name(orgChange.getName())
                .logoUrl(orgChange.getLogoUrl()).website(orgChange.getWebsite()).build();
        organizationRepository.save(orgDoc).subscribe();

        // 2. Trigger a bulk update for all events that embed this organization's info
        EventDocument.OrganizationInfo embeddedInfo = EventDocument.OrganizationInfo.builder()
                .id(orgChange.getId().toString()).name(orgChange.getName()).logoUrl(orgChange.getLogoUrl()).build();
        eventRepository.updateOrganizationInfoInEvents(orgChange.getId().toString(), embeddedInfo).subscribe(count ->
                log.info("Updated embedded organization info for {} events.", count)
        );
    }

    private void processCategoryChange(String payload) throws Exception {
        JsonNode message = objectMapper.readTree(payload).path("payload");
        String operation = message.path("op").asText();

        if ("d".equals(operation)) {
            String catId = message.path("before").path("id").asText();
            categoryRepository.deleteById(catId).subscribe();
            // In a real system, you might trigger a process to re-categorize events
            return;
        }

        CategoryChangePayload catChange = objectMapper.treeToValue(message.path("after"), CategoryChangePayload.class);

        // We need the parent category's name for our denormalized document
        Mono<CategoryDocument> parentCategoryMono = catChange.getParentId() != null
                ? categoryRepository.findById(catChange.getParentId().toString())
                : Mono.empty();

        parentCategoryMono.defaultIfEmpty(CategoryDocument.builder().build()) // Handle case where parent is null
                .flatMap(parentCat -> {
                    CategoryDocument catDoc = CategoryDocument.builder()
                            .id(catChange.getId().toString())
                            .name(catChange.getName())
                            .parentName(parentCat.getName()) // This will be null if no parent
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
                .subscribe(count ->
                        log.info("Updated embedded category info for {} events.", count)
                );
    }
}
