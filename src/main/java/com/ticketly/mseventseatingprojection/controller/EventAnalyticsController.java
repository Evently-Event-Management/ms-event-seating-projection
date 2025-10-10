package com.ticketly.mseventseatingprojection.controller;

import com.ticketly.mseventseatingprojection.dto.analytics.EventAnalyticsDTO;
import com.ticketly.mseventseatingprojection.dto.analytics.SessionAnalyticsDTO;
import com.ticketly.mseventseatingprojection.dto.analytics.SessionSummaryDTO;
import com.ticketly.mseventseatingprojection.model.EventDocument;
import com.ticketly.mseventseatingprojection.model.EventTrendingDocument;
import com.ticketly.mseventseatingprojection.service.EventAnalyticsService;
import com.ticketly.mseventseatingprojection.service.EventQueryService;
import com.ticketly.mseventseatingprojection.service.EventTrendingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/v1/analytics")
@RequiredArgsConstructor
@Tag(name = "Event Analytics", description = "APIs for event and session analytics")
@Slf4j
public class EventAnalyticsController {

    private final EventAnalyticsService eventAnalyticsService;
    private final EventTrendingService eventTrendingService;

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
    
    /**
     * Get top trending events.
     *
     * @param limit The maximum number of events to return.
     * @return Flux emitting trending events ordered by trending score.
     */
    @GetMapping("/trending")
    @Operation(summary = "Get top trending events",
            description = "Returns events sorted by their trending score which is calculated based on views, purchases, and reservations")
    public Flux<EventTrendingDocument> getTopTrendingEvents(@RequestParam(defaultValue = "10") int limit) {
        log.info("Requested top {} trending events", limit);
        return eventTrendingService.getTopTrendingEvents(limit);
    }
}
