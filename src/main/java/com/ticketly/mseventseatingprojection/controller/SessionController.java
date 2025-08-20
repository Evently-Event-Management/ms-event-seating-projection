package com.ticketly.mseventseatingprojection.controller;

import com.ticketly.mseventseatingprojection.model.EventDocument.SessionSeatingMapInfo;
import com.ticketly.mseventseatingprojection.service.EventQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/v1/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final EventQueryService eventQueryService;

    /**
     * Retrieves the seating map for a specific session by its ID.
     *
     * @param sessionId The ID of the session
     * @return A Mono emitting the session's seating map or a not found response
     */
    @GetMapping("/{sessionId}/seating-map")
    public Mono<ResponseEntity<SessionSeatingMapInfo>> getSessionSeatingMap(@PathVariable String sessionId) {
        return eventQueryService.getSessionSeatingMap(sessionId)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
}
