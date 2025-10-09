package com.ticketly.mseventseatingprojection.controller;

import com.ticketly.mseventseatingprojection.model.EventTrackingDocument;
import com.ticketly.mseventseatingprojection.service.EventTrackingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

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
    
    @PostMapping("/{eventId}/views/track")
    @Operation(summary = "Track a view for an event by automatically detecting device type from User-Agent")
    public Mono<ResponseEntity<EventTrackingDocument>> trackEventView(
            @PathVariable String eventId,
            ServerHttpRequest request) {
        
        String detectedDeviceType = com.ticketly.mseventseatingprojection.util.UserAgentUtil.getDeviceType(request);
        
        log.debug("REST request to track view for event: {}, detected device type: {}", 
                eventId, detectedDeviceType);
                
        return eventTrackingService.incrementViewCount(eventId, detectedDeviceType)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
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