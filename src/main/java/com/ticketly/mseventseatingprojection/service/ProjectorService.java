package com.ticketly.mseventseatingprojection.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketly.mseventseatingprojection.dto.SessionSeatingMapDTO;
import com.ticketly.mseventseatingprojection.model.EventDocument;
import com.ticketly.mseventseatingprojection.repository.EventRepository;
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
    private final ObjectMapper objectMapper;

    private final EventProjectionMapper eventProjectionMapper; // Projection → Document
    private final SessionSeatingMapper sessionSeatingMapper;   // Seating DTO → Document

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
}
