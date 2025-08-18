package com.ticketly.mseventseatingprojection.controller;

import com.ticketly.mseventseatingprojection.dto.read.EventBasicInfoDTO;
import com.ticketly.mseventseatingprojection.dto.read.EventThumbnailDTO;
import com.ticketly.mseventseatingprojection.service.EventQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;

@RestController
@RequestMapping("/v1/events")
@RequiredArgsConstructor
public class EventQueryController {

    private final EventQueryService eventQueryService;

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

    @GetMapping("/{eventId}/basic-info")
    public Mono<ResponseEntity<EventBasicInfoDTO>> getBasicEventInfo(@PathVariable String eventId) {
        return eventQueryService.getBasicEventInfo(eventId)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
}
