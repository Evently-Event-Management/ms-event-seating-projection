package com.ticketly.mseventseatingprojection.controller;

import com.ticketly.mseventseatingprojection.dto.analytics.EventAnalyticsBatchRequest;
import com.ticketly.mseventseatingprojection.dto.analytics.EventAnalyticsDTO;
import com.ticketly.mseventseatingprojection.dto.analytics.SessionAnalyticsDTO;
import com.ticketly.mseventseatingprojection.dto.analytics.SessionSummaryDTO;
import com.ticketly.mseventseatingprojection.service.EventAnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/v1/analytics")
@RequiredArgsConstructor
@Tag(name = "Event Analytics", description = "APIs for event and session analytics")
@Slf4j
public class EventAnalyticsController {

    private final EventAnalyticsService eventAnalyticsService;

    /**
     * Get comprehensive analytics for an event.
     *
     * @param eventId The event ID.
     * @return Mono emitting ResponseEntity with EventAnalyticsDTO or not found.
     */
    @GetMapping("/events/{eventId}")
    @Operation(summary = "Get comprehensive analytics for an event",
            description = "Returns aggregated analytics across all sessions including revenue, tickets sold, and capacity metrics. For event view statistics, see /v1/events/{eventId}/views/stats")
    public Mono<ResponseEntity<EventAnalyticsDTO>> getEventAnalytics(@PathVariable String eventId, @AuthenticationPrincipal Jwt jwt) {
        log.info("User {} requested analytics for event {}", jwt.getSubject(), eventId);
        return eventAnalyticsService.getEventAnalytics(eventId, jwt.getSubject())
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
    
    /**
     * Get comprehensive analytics for multiple events in batch.
     *
     * @param req List of event IDs.
     * @return Flux emitting EventAnalyticsDTO for each event.
     */
    @PostMapping("/events/batch/individual")
    @Operation(summary = "Get comprehensive analytics for multiple events in batch",
            description = "Returns aggregated analytics for multiple events in a single request. Only returns data for events the user is authorized to access.")
    public Flux<EventAnalyticsDTO> getBatchEventAnalytics(@RequestBody EventAnalyticsBatchRequest req, @AuthenticationPrincipal Jwt jwt) {
        log.info("User {} requested batch analytics for {} events", jwt.getSubject(), req.getEventIds().size());
        return eventAnalyticsService.getBatchEventAnalytics(req.getEventIds(), jwt.getSubject());
    }

    /**
     * Get basic analytics for all sessions in an event.
     *
     * @param eventId The event ID.
     * @return Flux emitting SessionSummaryDTO for each session.
     */
    @GetMapping("/events/{eventId}/sessions")
    @Operation(summary = "Get basic analytics for all sessions in an event",
            description = "Returns basic analytics for each session including revenue, tickets sold, and capacity")
    public Flux<SessionSummaryDTO> getAllSessionsAnalytics(@PathVariable String eventId, @AuthenticationPrincipal Jwt jwt) {
        log.info("User {} requested session summaries for event {}", jwt.getSubject(), eventId);
        return eventAnalyticsService.getAllSessionsAnalytics(eventId, jwt.getSubject());
    }

    /**
     * Get detailed analytics for a specific session.
     *
     * @param eventId The event ID.
     * @param sessionId The session ID.
     * @return Mono emitting ResponseEntity with SessionAnalyticsDTO or not found.
     */
    @GetMapping("/events/{eventId}/sessions/{sessionId}")
    @Operation(summary = "Get detailed analytics for a specific session",
            description = "Returns detailed analytics for a specific session including revenue, capacity, and seat status breakdown")
    public Mono<ResponseEntity<SessionAnalyticsDTO>> getSessionAnalytics(
            @PathVariable String eventId,
            @PathVariable String sessionId,
            @AuthenticationPrincipal Jwt jwt) {
        log.info("User {} requested analytics for session {} of event {}", jwt.getSubject(), sessionId, eventId);
        return eventAnalyticsService.getSessionAnalytics(eventId, sessionId , jwt.getSubject())
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
}
