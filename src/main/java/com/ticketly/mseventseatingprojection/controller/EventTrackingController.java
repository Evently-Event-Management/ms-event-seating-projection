package com.ticketly.mseventseatingprojection.controller;

import com.ticketly.mseventseatingprojection.dto.analytics.EventViewsStatsDTO;
import com.ticketly.mseventseatingprojection.model.EventTrackingDocument;
import com.ticketly.mseventseatingprojection.service.EventTrackingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

@RestController
@RequestMapping("/v1/events")
@RequiredArgsConstructor
@Tag(name = "Event Views Analytics", description = "APIs for tracking and retrieving event views statistics")
@Slf4j
public class EventTrackingController {

    private final EventTrackingService eventTrackingService;
    
    @PostMapping("/{eventId}/views/increment")
    @Operation(summary = "Increment view count for an event by device type")
    public Mono<ResponseEntity<EventTrackingDocument>> incrementViewCount(
            @PathVariable String eventId,
            @RequestParam(required = true) String deviceType) {
        log.debug("REST request to increment view count for event: {}, device type: {}", eventId, deviceType);
        return eventTrackingService.incrementViewCount(eventId, deviceType)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
    
    @PostMapping("/{eventId}/orders/increment")
    @Operation(summary = "Increment order count for an event")
    public Mono<ResponseEntity<EventTrackingDocument>> incrementOrderCount(
            @PathVariable String eventId) {
        log.debug("REST request to increment order count for event: {}", eventId);
        return eventTrackingService.incrementOrderCount(eventId)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/{eventId}/views/stats")
    @Operation(summary = "Get event views statistics for a date range")
    public Mono<ResponseEntity<EventViewsStatsDTO>> getEventViewsStats(
            @PathVariable String eventId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        
        LocalDate start = fromDate != null ? fromDate : LocalDate.now().minusMonths(1);
        LocalDate end = toDate != null ? toDate : LocalDate.now();
        
        log.debug("REST request to get event views stats for event: {}, date range: {} to {}", eventId, start, end);
        return eventTrackingService.getEventViewsStats(eventId, start, end)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/{eventId}/views/all")
    @Operation(summary = "Get all analytics data for an event")
    @PreAuthorize("hasRole('ADMIN')")
    public Flux<EventTrackingDocument> getAllEventAnalytics(@PathVariable String eventId) {
        log.debug("REST request to get all analytics for event: {}", eventId);
        return eventTrackingService.getAllEventAnalytics(eventId);
    }
    
    @DeleteMapping("/{eventId}/views")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete all analytics for an event")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<Void> deleteEventAnalytics(@PathVariable String eventId) {
        log.debug("REST request to delete all analytics for event: {}", eventId);
        return eventTrackingService.deleteEventAnalytics(eventId);
    }
}