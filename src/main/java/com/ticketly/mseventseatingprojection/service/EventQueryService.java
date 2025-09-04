package com.ticketly.mseventseatingprojection.service;

import com.ticketly.mseventseatingprojection.dto.SessionInfoDTO;
import com.ticketly.mseventseatingprojection.dto.read.EventBasicInfoDTO;
import com.ticketly.mseventseatingprojection.dto.read.EventThumbnailDTO;
import com.ticketly.mseventseatingprojection.exception.ResourceNotFoundException;
import com.ticketly.mseventseatingprojection.model.EventDocument;
import com.ticketly.mseventseatingprojection.model.EventDocument.SessionSeatingMapInfo;
import com.ticketly.mseventseatingprojection.repository.EventReadRepositoryCustomImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventQueryService {

    private final EventReadRepositoryCustomImpl eventReadRepository;

    public Mono<Page<EventThumbnailDTO>> searchEvents(
            String searchTerm, String categoryId, Double longitude, Double latitude,
            Integer radiusKm, Instant dateFrom, Instant dateTo,
            BigDecimal priceMin, BigDecimal priceMax, Pageable pageable
    ) {
        log.debug("searchEvents called with term={}, categoryId={}, location=({},{}), radius={}, dateFrom={}, dateTo={}, priceMin={}, priceMax={}, pageable={}",
                searchTerm, categoryId, longitude, latitude, radiusKm, dateFrom, dateTo, priceMin, priceMax, pageable);

        return eventReadRepository.searchEvents(
                        searchTerm, categoryId, longitude, latitude, radiusKm,
                        dateFrom, dateTo, priceMin, priceMax, pageable
                ).map(eventPage -> eventPage.map(this::mapToThumbnailDTO))
                .doOnNext(page -> log.info("searchEvents result: totalElements={}, pageSize={}, pageNumber={}",
                        page.getTotalElements(), page.getSize(), page.getNumber()));
    }

    private EventThumbnailDTO mapToThumbnailDTO(EventDocument event) {
        // Find the earliest upcoming session
        EventDocument.SessionInfo earliestSession = event.getSessions().stream()
                .filter(s -> s.getStartTime().isAfter(Instant.now()))
                .min(Comparator.comparing(EventDocument.SessionInfo::getStartTime))
                .orElse(event.getSessions().stream().findFirst().orElse(null));

        // Find the lowest priced tier
        BigDecimal startingPrice = event.getTiers().stream()
                .map(EventDocument.TierInfo::getPrice)
                .min(Comparator.naturalOrder())
                .orElse(BigDecimal.ZERO);

        EventThumbnailDTO.EarliestSessionInfo sessionInfo = null;
        if (earliestSession != null) {
            sessionInfo = EventThumbnailDTO.EarliestSessionInfo.builder()
                    .startTime(earliestSession.getStartTime())
                    .venueName(earliestSession.getVenueDetails() != null ? earliestSession.getVenueDetails().getName() : "Online")
                    .city(extractCity(earliestSession.getVenueDetails()))
                    .build();
        }

        return EventThumbnailDTO.builder()
                .id(event.getId())
                .title(event.getTitle())
                .coverPhotoUrl(event.getCoverPhotos() != null && !event.getCoverPhotos().isEmpty()
                        ? event.getCoverPhotos().getFirst()
                        : null)
                .organizationName(event.getOrganization().getName())
                .categoryName(event.getCategory().getName())
                .earliestSession(sessionInfo)
                .startingPrice(startingPrice)
                .build();
    }

    private String extractCity(EventDocument.VenueDetailsInfo venue) {
        if (venue == null || venue.getAddress() == null || venue.getAddress().isBlank()) {
            return null;
        }
        // Simple city extraction logic, can be improved
        String[] parts = venue.getAddress().split(",");
        return parts.length > 1 ? parts[1].trim() : parts[0].trim();
    }

    /**
     * Fetches basic event info by event ID (excluding tiers and sessions).
     *
     * @param eventId The ID of the event.
     * @return Mono emitting EventBasicInfoDTO or empty if not found.
     */
    public Mono<EventBasicInfoDTO> getBasicEventInfo(String eventId) {
        log.debug("getBasicEventInfo called for eventId={}", eventId);
        return eventReadRepository.findEventBasicInfoById(eventId)
                .map(event -> EventBasicInfoDTO.builder()
                        .id(event.getId())
                        .title(event.getTitle())
                        .description(event.getDescription())
                        .overview(event.getOverview())
                        .coverPhotos(event.getCoverPhotos())
                        .organization(event.getOrganization() != null ? EventBasicInfoDTO.OrganizationInfo.builder()
                                .id(event.getOrganization().getId())
                                .name(event.getOrganization().getName())
                                .logoUrl(event.getOrganization().getLogoUrl())
                                .build() : null)
                        .category(event.getCategory() != null ? EventBasicInfoDTO.CategoryInfo.builder()
                                .id(event.getCategory().getId())
                                .name(event.getCategory().getName())
                                .parentName(event.getCategory().getParentName())
                                .build() : null)
                        .tiers(event.getTiers() != null ? event.getTiers().stream()
                                .map(tier -> EventBasicInfoDTO.TierInfo.builder()
                                        .id(tier.getId())
                                        .name(tier.getName())
                                        .price(tier.getPrice())
                                        .color(tier.getColor())
                                        .build())
                                .collect(Collectors.toList()) : Collections.emptyList())
                        .build())
                .doOnNext(dto -> log.info("getBasicEventInfo outcome for eventId={}: title={}", eventId, dto.getTitle()))
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Event", "id", eventId)));
    }

    public Mono<Page<SessionInfoDTO>> findSessionsBasicInfoByEventId(String eventId, Pageable pageable) {
        log.debug("findSessionsByEventId called for eventId={}, pageable={}", eventId, pageable);
        return eventReadRepository.findSessionsByEventId(eventId, pageable)
                .map(sessionPage -> sessionPage.map(this::mapToSessionInfoDTO))
                .doOnNext(page -> log.info("findSessionsByEventId outcome for eventId={}: totalSessionsOnPage={}", eventId, page.getNumberOfElements()));
    }

    private SessionInfoDTO mapToSessionInfoDTO(EventDocument.SessionInfo session) {
        SessionInfoDTO.VenueDetailsInfo venueDetailsDTO = null;
        if (session.getVenueDetails() != null) {
            venueDetailsDTO = SessionInfoDTO.VenueDetailsInfo.builder()
                    .name(session.getVenueDetails().getName())
                    .address(session.getVenueDetails().getAddress())
                    .onlineLink(session.getVenueDetails().getOnlineLink())
                    .location(session.getVenueDetails().getLocation())
                    .build();
        }

        return SessionInfoDTO.builder()
                .id(session.getId())
                .startTime(session.getStartTime())
                .endTime(session.getEndTime())
                .status(session.getStatus())
                .sessionType(session.getSessionType())
                .venueDetails(venueDetailsDTO)
                .salesStartTime(session.getSalesStartTime())
                .build();
    }

    /**
     * Fetches the seating map information for a specific session by its ID.
     *
     * @param sessionId The ID of the session
     * @return A Mono emitting the seating map information or empty if not found
     */
    public Mono<SessionSeatingMapInfo> getSessionSeatingMap(String sessionId) {
        log.debug("getSessionSeatingMap called for sessionId={}", sessionId);
        return eventReadRepository.findSeatingMapBySessionId(sessionId)
                .doOnSuccess(map -> {
                    if (map != null) {
                        log.info("getSessionSeatingMap outcome for sessionId={}: seatingMapFound=true", sessionId);
                    } else {
                        log.info("getSessionSeatingMap outcome for sessionId={}: seatingMapFound=false", sessionId);
                    }
                })
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Session seating map", "session_id", sessionId)));
    }

    /**
     * Fetches sessions within a specific date range for a given event ID.
     *
     * @param eventId  The ID of the event.
     * @param fromDate The start date of the range.
     * @param toDate   The end date of the range.
     * @return A Flux emitting the session information.
     */
    public Flux<SessionInfoDTO> findSessionsInRange(String eventId, Instant fromDate, Instant toDate) {
        log.info("findSessionsInRange called: eventId={}, from={}, to={} (fetching sessions)", eventId, fromDate, toDate);
        return eventReadRepository.findSessionsInRange(eventId, fromDate, toDate)
                .map(this::mapToSessionInfoDTO);
    }

    /**
     * Fetches a single session by its ID without the seating map layout data.
     *
     * @param sessionId The ID of the session to fetch.
     * @return A Mono emitting the session information or empty if not found.
     */
    public Mono<SessionInfoDTO> getSessionById(String sessionId) {
        log.debug("getSessionById called for sessionId={}", sessionId);
        return eventReadRepository.findSessionBasicInfoById(sessionId)
                .flatMap(eventDocument -> {
                    // Find the specific session within the event document
                    return Mono.justOrEmpty(eventDocument.getSessions().stream()
                            .filter(s -> s.getId().equals(sessionId))
                            .findFirst()
                            .map(this::mapToSessionInfoDTO));
                })
                .doOnNext(dto -> log.info("getSessionById outcome for sessionId={}: found=true startTime={}", sessionId, dto.getStartTime()));
    }
}
