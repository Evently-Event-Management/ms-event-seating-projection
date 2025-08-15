package com.ticketly.mseventseatingprojection.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketly.mseventseatingprojection.dto.SessionSeatingMapDTO;
import com.ticketly.mseventseatingprojection.model.EventDocument;
import com.ticketly.mseventseatingprojection.repository.EventRepository;
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
    private final EventDocumentMapper eventDocumentMapper;

    public Mono<Void> projectFullEvent(UUID eventId) {
        log.info("Projecting full event for ID: {}", eventId);
        return eventProjectionClient.getEventProjectionData(eventId)
                .flatMap(dto -> eventRepository.save(eventDocumentMapper.toEventDocument(dto)))
                .then();
    }

    public Mono<Void> deleteEvent(UUID eventId) {
        log.info("Deleting event {} from read model.", eventId);
        return eventRepository.deleteById(eventId.toString());
    }

    public Mono<Void> projectSessionUpdate(UUID eventId, UUID sessionId) {
        log.info("Projecting session update for event ID: {} and session ID: {}", eventId, sessionId);
        return eventProjectionClient.getSessionProjectionData(sessionId)
                .flatMap(sessionDto -> {
                    EventDocument.SessionInfo sessionInfo = eventDocumentMapper.toSessionInfo(sessionDto);
                    return eventRepository.updateSessionInEvent(eventId.toString(), sessionInfo);
                })
                .then();
    }

    public Mono<Void> projectSeatingMapPatch(UUID eventId, UUID sessionId, String layoutJson) {
        log.info("Projecting seating map update for event ID: {} and session ID: {}", eventId, sessionId);

        return eventRepository.findById(eventId.toString())
                .flatMap(eventDocument -> {
                    try {
                        SessionSeatingMapDTO seatingMapDto = objectMapper.readValue(layoutJson, SessionSeatingMapDTO.class);

                        Map<String, EventDocument.TierInfo> tierInfoMap = eventDocument.getTiers().stream()
                                .collect(Collectors.toMap(EventDocument.TierInfo::getId, Function.identity()));

                        EventDocument.SessionSeatingMapInfo seatingMapInfo = eventDocumentMapper.toSeatingMapInfo(seatingMapDto, tierInfoMap);
                        return eventRepository.updateSeatingMapInSession(eventId.toString(), sessionId.toString(), seatingMapInfo);
                    } catch (Exception e) {
                        log.error("Failed to process seating map update for session {}", sessionId, e);
                        return Mono.empty();
                    }
                })
                .then();
    }
}
