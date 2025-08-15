package com.ticketly.mseventseatingprojection.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketly.mseventseatingprojection.dto.EventChangePayload;
import com.ticketly.mseventseatingprojection.dto.SeatingMapChangePayload;
import com.ticketly.mseventseatingprojection.dto.SessionChangePayload;
import com.ticketly.mseventseatingprojection.model.EventDocument;
import com.ticketly.mseventseatingprojection.repository.EventRepository;
import com.ticketly.mseventseatingprojection.service.ProjectorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @KafkaListener(topics = "dbz.ticketly.public.events")
    public void onEventChange(String payload) {
        try {
            JsonNode message = objectMapper.readTree(payload).path("payload");
            if (message.path("op").asText().equals("d")) { // Handle delete
                projectorService.deleteEvent(UUID.fromString(message.path("before").path("id").asText())).subscribe();
                return;
            }

            EventChangePayload eventChange = objectMapper.treeToValue(message.path("after"), EventChangePayload.class);
            if ("APPROVED".equals(eventChange.getStatus())) {
                projectorService.projectFullEvent(eventChange.getId()).subscribe();
            } else {
                projectorService.deleteEvent(eventChange.getId()).subscribe();
            }
        } catch (Exception e) {
            log.error("Error processing event change", e);
        }
    }

    @KafkaListener(topics = "dbz.ticketly.public.event_sessions")
    public void onSessionChange(String payload) {
        try {
            JsonNode message = objectMapper.readTree(payload).path("payload");
            if (message.path("op").asText().equals("d")) return; // Deletes are handled by event cascade

            SessionChangePayload sessionChange = objectMapper.treeToValue(message.path("after"), SessionChangePayload.class);

            // Only project the session if the parent event already exists in the read model
            eventReadRepository.existsById(sessionChange.getEventId().toString())
                    .filter(exists -> exists)
                    .flatMap(exists -> projectorService.projectSessionUpdate(sessionChange.getEventId(), sessionChange.getId()))
                    .subscribe();
        } catch (Exception e) {
            log.error("Error processing session change", e);
        }
    }

    @KafkaListener(topics = "dbz.ticketly.public.session_seating_maps")
    public void onSeatingMapChange(String payload) {
        try {
            JsonNode message = objectMapper.readTree(payload).path("payload");
            if (message.path("op").asText().equals("d")) return;

            SeatingMapChangePayload mapChange = objectMapper.treeToValue(message.path("after"), SeatingMapChangePayload.class);

            // Find the parent event for this session
            eventReadRepository.findEventIdBySessionId(mapChange.getSessionId().toString())
                    .flatMap(eventDocument -> {
                        // Check the session status to decide on the update strategy
                        EventDocument.SessionInfo session = eventDocument.getSessions().stream()
                                .filter(s -> s.getId().equals(mapChange.getSessionId().toString()))
                                .findFirst().orElse(null);

                        if (session == null) return Mono.empty();

                        if ("ON_SALE".equals(session.getStatus())) {
                            // High-frequency update: use the efficient "read-side join"
                            return projectorService.projectSeatingMapPatch(UUID.fromString(eventDocument.getId()), mapChange.getSessionId(), mapChange.getLayoutData());
                        } else {
                            // Low-frequency update (organizer change): use the "signal and fetch" pattern
                            return projectorService.projectSessionUpdate(UUID.fromString(eventDocument.getId()), mapChange.getSessionId());
                        }
                    }).subscribe();
        } catch (Exception e) {
            log.error("Error processing seating map change", e);
        }
    }
}
