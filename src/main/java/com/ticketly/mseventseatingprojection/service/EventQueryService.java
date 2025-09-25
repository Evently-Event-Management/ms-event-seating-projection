package com.ticketly.mseventseatingprojection.service;

import com.ticketly.mseventseatingprojection.dto.SessionInfoDTO;
import com.ticketly.mseventseatingprojection.dto.read.DiscountDetailsDTO;
import com.ticketly.mseventseatingprojection.dto.read.EventBasicInfoDTO;
import com.ticketly.mseventseatingprojection.dto.read.EventThumbnailDTO;
import com.ticketly.mseventseatingprojection.exception.ResourceNotFoundException;
import com.ticketly.mseventseatingprojection.model.EventDocument;
import com.ticketly.mseventseatingprojection.model.EventDocument.SessionSeatingMapInfo;
import com.ticketly.mseventseatingprojection.repository.EventReadRepositoryCustomImpl;
import dto.projection.discount.BogoDiscountParamsDTO;
import dto.projection.discount.DiscountParametersDTO;
import dto.projection.discount.FlatOffDiscountParamsDTO;
import dto.projection.discount.PercentageDiscountParamsDTO;
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

    /**
     * Searches for events based on various filters and returns a paginated list of event thumbnails.
     *
     * @param searchTerm Search keyword for event title or description.
     * @param categoryId Category ID to filter events.
     * @param longitude  Longitude for location-based search.
     * @param latitude   Latitude for location-based search.
     * @param radiusKm   Radius in kilometers for location-based search.
     * @param dateFrom   Start date filter.
     * @param dateTo     End date filter.
     * @param priceMin   Minimum price filter.
     * @param priceMax   Maximum price filter.
     * @param pageable   Pagination information.
     * @return Mono emitting a page of EventThumbnailDTO.
     */
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

    /**
     * Fetches paginated basic session info for a given event ID.
     *
     * @param eventId  The ID of the event.
     * @param pageable Pagination information.
     * @return A Mono emitting a Page of SessionInfoDTO.
     */
    public Mono<Page<SessionInfoDTO>> findSessionsBasicInfoByEventId(String eventId, Pageable pageable) {
        log.debug("findSessionsByEventId called for eventId={}, pageable={}", eventId, pageable);
        return eventReadRepository.findSessionsByEventId(eventId, pageable)
                .map(sessionPage -> sessionPage.map(this::mapToSessionInfoDTO))
                .doOnNext(page -> log.info("findSessionsByEventId outcome for eventId={}: totalSessionsOnPage={}", eventId, page.getNumberOfElements()));
    }

    /**
     * Maps EventDocument.SessionInfo to SessionInfoDTO.
     *
     * @param session The session info from the event document.
     * @return The mapped SessionInfoDTO.
     */
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

    /**
     * Fetches all active, public discounts for a given event and session.
     *
     * @param eventId The ID of the event.
     * @param sessionId The ID of the session.
     * @return A Flux of DiscountDetailsDTOs.
     */
    public Flux<DiscountDetailsDTO> getPublicDiscountsForSession(String eventId, String sessionId) {
        log.debug("getPublicDiscountsForSession called for eventId={}, sessionId={}", eventId, sessionId);
        return eventReadRepository.findPublicDiscountsByEventAndSession(eventId, sessionId)
                .map(this::mapToDiscountDetailsDTO);
    }

    /**
     * Fetches a single active discount by its code, verifying it's applicable to the given session.
     *
     * @param sessionId The ID of the session.
     * @param code The discount code.
     * @return A Mono emitting the DiscountDetailsDTO or empty if not found/applicable.
     */
    public Mono<DiscountDetailsDTO> getDiscountByCodeForSession(String sessionId, String code) {
        log.debug("getDiscountByCodeForSession called for sessionId={}, code={}", sessionId, code);
        return eventReadRepository.findActiveDiscountByCodeAndSession(sessionId, code)
                .map(this::mapToDiscountDetailsDTO)
                .doOnSuccess(dto -> {
                    if (dto != null) log.info("Discount '{}' found for session {}", code, sessionId);
                    else log.info("Discount '{}' not found or not applicable for session {}", code, sessionId);
                });
    }

    /**
     *
     * @param eventId The ID of the event.
     * @param sessionId The ID of the session.
     * @param code The discount code.
     * @return A Mono emitting the DiscountDetailsDTO or empty if not found/applicable.
     */
    public Mono<DiscountDetailsDTO> getDiscountByCodeForEventAndSession(String eventId, String sessionId, String code) {
        log.debug("getDiscountByCodeForEventAndSession called for eventId={}, sessionId={}, code={}", eventId, sessionId, code);
        return eventReadRepository.findActiveDiscountByCodeAndEventAndSession(eventId, sessionId, code)
                .map(this::mapToDiscountDetailsDTO)
                .doOnSuccess(dto -> {
                    if (dto != null) log.info("Discount '{}' found for event {} and session {}", code, eventId, sessionId);
                    else log.info("Discount '{}' not found or not applicable for session {}", code, sessionId);
                });
    }

    private DiscountDetailsDTO mapToDiscountDetailsDTO(EventDocument.DiscountInfo discountInfo) {
        if (discountInfo == null) {
            return null;
        }
        return DiscountDetailsDTO.builder()
                .id(discountInfo.getId())
                .code(discountInfo.getCode())
                .parameters(mapToDiscountParameters(discountInfo.getParameters()))
                .isActive(discountInfo.isActive())
                .isPublic(discountInfo.isPublic())
                .activeFrom(discountInfo.getActiveFrom())
                .expiresAt(discountInfo.getExpiresAt())
                .build();
    }

    private DiscountParametersDTO mapToDiscountParameters(EventDocument.DiscountParametersInfo paramsInfo) {
        if (paramsInfo == null || paramsInfo.getType() == null) {
            return null;
        }

        return switch (paramsInfo.getType()) {
            case PERCENTAGE -> new PercentageDiscountParamsDTO(
                    paramsInfo.getType(),
                    paramsInfo.getPercentage()
            );
            case FLAT_OFF -> new FlatOffDiscountParamsDTO(
                    paramsInfo.getType(),
                    paramsInfo.getAmount(),
                    paramsInfo.getCurrency()
            );
            case BUY_N_GET_N_FREE -> new BogoDiscountParamsDTO(
                    paramsInfo.getType(),
                    paramsInfo.getBuyQuantity(),
                    paramsInfo.getGetQuantity()
            );
        };
    }
}
