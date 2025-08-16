package com.ticketly.mseventseatingprojection.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketly.mseventseatingprojection.dto.*;
import com.ticketly.mseventseatingprojection.repository.EventRepository;
import com.ticketly.mseventseatingprojection.service.ProjectorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import model.EventDocument;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
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

    // ✅ A single listener for all Debezium topics
    @KafkaListener(topics = {
            "dbz.ticketly.public.events",
            "dbz.ticketly.public.event_sessions",
            "dbz.ticketly.public.session_seating_maps",
            "dbz.ticketly.public.organizations",
            "dbz.ticketly.public.categories",
            "dbz.ticketly.public.event_cover_photos"
    })
    public void onDebeziumEvent(ConsumerRecord<String, String> record) {
        String topic = record.topic();
        String payload = record.value();

        log.debug("Received message on topic: {}", topic);
        if (payload == null || payload.isBlank()) {
            log.debug("Received tombstone record on topic {}. Ignoring.", topic);
            return;
        }
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
            } else if (topic.endsWith(".event_cover_photos")) {
                processCoverPhotoChange(payload);
            } else {
                log.warn("Unhandled topic: {}", topic);
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

        if ("d".equals(operation)) {
            String orgId = message.path("before").path("id").asText();
            projectorService.deleteOrganization(orgId).subscribe();
            return;
        }

        OrganizationChangePayload orgChange = objectMapper.treeToValue(message.path("after"), OrganizationChangePayload.class);
        projectorService.projectOrganizationChange(orgChange).subscribe();
    }

    private void processCategoryChange(String payload) throws Exception {
        JsonNode message = objectMapper.readTree(payload).path("payload");
        String operation = message.path("op").asText();

        if ("d".equals(operation)) {
            String catId = message.path("before").path("id").asText();
            projectorService.deleteCategory(catId).subscribe();
            return;
        }

        CategoryChangePayload catChange = objectMapper.treeToValue(message.path("after"), CategoryChangePayload.class);
        projectorService.projectCategoryChange(catChange).subscribe();
    }

    private void processCoverPhotoChange(String payload) throws Exception {
        JsonNode message = objectMapper.readTree(payload).path("payload");
        String operation = message.path("op").asText();

        if ("u".equals(operation)) return; // Ignore updates, not relevant for this table

        // Determine which part of the payload to use based on the operation
        JsonNode dataNode = "d".equals(operation) ? message.path("before") : message.path("after");
        if (dataNode.isMissingNode()) return;

        CoverPhotoChangePayload photoChange = objectMapper.treeToValue(dataNode, CoverPhotoChangePayload.class);

        // ✅ CRITICAL: Only process the change if the parent event already exists in the read model.
        // This prevents race conditions during initial event creation.
        eventReadRepository.existsById(photoChange.getEventId().toString())
                .filter(exists -> exists)
                .flatMap(exists -> {
                    if ("c".equals(operation)) {
                        log.info("Projecting cover photo addition for event ID: {}", photoChange.getEventId());
                        return projectorService.projectCoverPhotoAdded(photoChange.getEventId(), photoChange.getPhotoUrl());
                    } else { // 'd' for delete
                        log.info("Projecting cover photo removal for event ID: {}", photoChange.getEventId());
                        return projectorService.projectCoverPhotoRemoved(photoChange.getEventId(), photoChange.getPhotoUrl());
                    }
                })
                .subscribe();
    }
}
