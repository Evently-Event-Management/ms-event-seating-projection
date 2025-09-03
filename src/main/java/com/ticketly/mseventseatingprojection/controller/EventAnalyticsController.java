package com.ticketly.mseventseatingprojection.controller;

import com.ticketly.mseventseatingprojection.dto.analytics.EventAnalyticsDTO;
import com.ticketly.mseventseatingprojection.dto.analytics.SessionAnalyticsDTO;
import com.ticketly.mseventseatingprojection.dto.analytics.SessionSummaryDTO;
import com.ticketly.mseventseatingprojection.service.EventAnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/v1/analytics")
@RequiredArgsConstructor
@Tag(name = "Event Analytics", description = "APIs for event and session analytics")
public class EventAnalyticsController {

    private final EventAnalyticsService eventAnalyticsService;

    @GetMapping("/events/{eventId}")
    @Operation(summary = "Get comprehensive analytics for an event",
            description = "Returns aggregated analytics across all sessions including revenue, tickets sold, and capacity metrics")
    public Mono<ResponseEntity<EventAnalyticsDTO>> getEventAnalytics(@PathVariable String eventId) {
        return eventAnalyticsService.getEventAnalytics(eventId)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping("/events/{eventId}/sessions")
    @Operation(summary = "Get basic analytics for all sessions in an event",
            description = "Returns basic analytics for each session including revenue, tickets sold, and capacity")
    public Flux<SessionSummaryDTO> getAllSessionsAnalytics(@PathVariable String eventId) {
        return eventAnalyticsService.getAllSessionsAnalytics(eventId);
    }

    @GetMapping("/events/{eventId}/sessions/{sessionId}")
    @Operation(summary = "Get detailed analytics for a specific session",
            description = "Returns detailed analytics for a specific session including revenue, capacity, and seat status breakdown")
    public Mono<ResponseEntity<SessionAnalyticsDTO>> getSessionAnalytics(
            @PathVariable String eventId,
            @PathVariable String sessionId) {
        return eventAnalyticsService.getSessionAnalytics(eventId, sessionId)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
}
