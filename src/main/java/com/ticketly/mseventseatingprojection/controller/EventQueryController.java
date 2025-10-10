package com.ticketly.mseventseatingprojection.controller;

import com.ticketly.mseventseatingprojection.dto.SessionInfoDTO;
import com.ticketly.mseventseatingprojection.dto.read.DiscountDetailsDTO;
import com.ticketly.mseventseatingprojection.dto.read.EventBasicInfoDTO;
import com.ticketly.mseventseatingprojection.dto.read.EventThumbnailDTO;
import com.ticketly.mseventseatingprojection.service.EventQueryService;
import com.ticketly.mseventseatingprojection.service.EventTrendingService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/v1/events")
@RequiredArgsConstructor
@EnableReactiveMethodSecurity
@Slf4j
public class EventQueryController {

    private final EventQueryService eventQueryService;
    private final EventTrendingService eventTrendingService;


    /**
     * Search for events based on filters and pagination.
     *
     * @param searchTerm Search keyword.
     * @param categoryId Category ID filter.
     * @param longitude Longitude for location filter.
     * @param latitude Latitude for location filter.
     * @param radiusKm Radius in kilometers for location filter.
     * @param dateFrom Start date filter.
     * @param dateTo End date filter.
     * @param priceMin Minimum price filter.
     * @param priceMax Maximum price filter.
     * @param pageable Pagination information.
     * @return Mono emitting ResponseEntity with a page of EventThumbnailDTO.
     */
    @GetMapping("/search")
    public Mono<ResponseEntity<Page<EventThumbnailDTO>>> searchEvents(
            @RequestParam(required = false) String searchTerm,
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) Double longitude,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Integer radiusKm,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dateTo,
            @RequestParam(required = false) BigDecimal priceMin,
            @RequestParam(required = false) BigDecimal priceMax,
            @PageableDefault(sort = "sessions.startTime", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return eventQueryService.searchEvents(
                searchTerm, categoryId, longitude, latitude, radiusKm,
                dateFrom, dateTo, priceMin, priceMax, pageable
        ).map(ResponseEntity::ok);
    }

    /**
     * Get basic event info by event ID.
     *
     * @param eventId The event ID.
     * @return Mono emitting ResponseEntity with EventBasicInfoDTO or not found.
     */
    @GetMapping("/{eventId}/basic-info")
    public Mono<ResponseEntity<EventBasicInfoDTO>> getBasicEventInfo(@PathVariable String eventId) {
        return eventQueryService.getBasicEventInfo(eventId)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /**
     * Get sessions for an event, paginated.
     *
     * @param eventId The event ID.
     * @param pageable Pagination information.
     * @return Mono emitting ResponseEntity with a page of SessionInfoDTO or not found.
     */
    @GetMapping("/{eventId}/sessions")
    public Mono<ResponseEntity<Page<SessionInfoDTO>>> getEventSessions(
            @PathVariable String eventId,
            @PageableDefault(sort = "startTime", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return eventQueryService.findSessionsBasicInfoByEventId(eventId, pageable)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /**
     * Get sessions for an event within a date range.
     *
     * @param eventId The event ID.
     * @param fromDate Start date.
     * @param toDate End date.
     * @return Mono emitting ResponseEntity with a list of SessionInfoDTO or not found.
     */
    @GetMapping("/{eventId}/sessions/sessions-in-range")
    public Mono<ResponseEntity<List<SessionInfoDTO>>> getSessionsInRange(
            @PathVariable String eventId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant toDate
    ) {
        return eventQueryService.findSessionsInRange(eventId, fromDate, toDate)
                .collectList()
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /**
     * Get all public, active discounts for a specific event session.
     *
     * @param eventId The event ID.
     * @param sessionId The session ID.
     * @return Mono emitting ResponseEntity with a list of public discounts.
     */
    @GetMapping("/{eventId}/sessions/{sessionId}/discounts/public")
    public Mono<ResponseEntity<List<DiscountDetailsDTO>>> getPublicDiscounts(
            @PathVariable String eventId,
            @PathVariable String sessionId
    ) {
        return eventQueryService.getPublicDiscountsForSession(eventId, sessionId)
                .collectList()
                .map(ResponseEntity::ok);
    }

    /**
     *
     *
     * @param eventId The event ID.
     * @param sessionId The session ID to validate against.
     * @param code The discount code to check.
     * @return Mono emitting ResponseEntity with the discount details or not found.
     */
    @GetMapping("/{eventId}/sessions/{sessionId}/discounts/code/{code}")
    @PreAuthorize("isAuthenticated()")
    public Mono<ResponseEntity<DiscountDetailsDTO>> getDiscountDetails(
            @PathVariable String eventId,
            @PathVariable String sessionId,
            @PathVariable String code
    ) {
        return eventQueryService.getDiscountByCodeForEventAndSession(eventId, sessionId, code)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /**
     * Get top trending events.
     *
     * @param limit The maximum number of events to return.
     * @return Flux emitting event thumbnails ordered by trending score.
     */
    @GetMapping("/trending")
    @Operation(summary = "Get top trending events",
            description = "Returns events sorted by their trending score which is calculated based on views, purchases, and reservations")
    public Flux<EventThumbnailDTO> getTopTrendingEvents(@RequestParam(defaultValue = "10") int limit) {
        log.info("Requested top {} trending events", limit);
        return eventTrendingService.getTopTrendingEventThumbnails(limit);
    }
    
    /**
     * Get the total count of all sessions in the database.
     *
     * @return Mono emitting ResponseEntity with the total count of sessions.
     */
    @GetMapping("/sessions/count")
    @Operation(summary = "Get total count of all sessions",
            description = "Returns the total count of all sessions across all events in the database")
    public Mono<ResponseEntity<Long>> getTotalSessionsCount() {
        log.info("Requested total count of all sessions");
        return eventQueryService.countAllSessions()
                .map(ResponseEntity::ok);
    }
}
