package com.ticketly.mseventseatingprojection.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketly.mseventseatingprojection.dto.*;
import com.ticketly.mseventseatingprojection.model.EventDocument;
import com.ticketly.mseventseatingprojection.repository.EventRepository;
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
}
