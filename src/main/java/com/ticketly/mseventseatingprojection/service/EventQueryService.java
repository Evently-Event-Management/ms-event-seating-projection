package com.ticketly.mseventseatingprojection.service;

import com.ticketly.mseventseatingprojection.dto.SessionInfoDTO;
import com.ticketly.mseventseatingprojection.dto.read.DiscountDetailsDTO;
import com.ticketly.mseventseatingprojection.dto.read.EventBasicInfoDTO;
import com.ticketly.mseventseatingprojection.dto.read.EventThumbnailDTO;
import com.ticketly.mseventseatingprojection.exception.ResourceNotFoundException;
import com.ticketly.mseventseatingprojection.model.EventDocument;
import com.ticketly.mseventseatingprojection.model.EventDocument.SessionSeatingMapInfo;
import com.ticketly.mseventseatingprojection.repository.EventReadRepositoryCustomImpl;
import com.ticketly.mseventseatingprojection.service.mapper.EventQueryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventQueryService {

    private final EventReadRepositoryCustomImpl eventReadRepository;
    private final EventQueryMapper eventMapper;

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
                )
                // ✅ Delegate mapping to the mapper component
                .map(eventPage -> eventPage.map(eventMapper::mapToThumbnailDTO))
                .doOnNext(page -> log.info("searchEvents result: totalElements={}, pageSize={}, pageNumber={}",
                        page.getTotalElements(), page.getSize(), page.getNumber()));
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
                .map(eventMapper::mapToBasicInfoDTO)
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

        // First fetch event discounts that will be needed for all sessions
        return eventReadRepository.findPublicDiscountsByEvent(eventId)
            .collectList()
            .flatMap(discounts -> {
                // Then fetch the sessions and map them with the discounts
                return eventReadRepository.findSessionsByEventId(eventId, pageable)
                    .map(sessionPage -> sessionPage.map(session ->
                        eventMapper.mapToSessionInfoDTO(session, discounts)))
                    .doOnNext(page -> log.info("findSessionsByEventId outcome for eventId={}: totalSessionsOnPage={}",
                        eventId, page.getNumberOfElements()));
            });
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

        // First fetch event discounts that will be needed for all sessions
        return eventReadRepository.findPublicDiscountsByEvent(eventId)
            .collectList()
            .flatMapMany(discounts -> {
                // Then fetch the sessions and map them with the discounts
                return eventReadRepository.findSessionsInRange(eventId, fromDate, toDate)
                    .map(session -> eventMapper.mapToSessionInfoDTO(session, discounts));
            });
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
                EventDocument.SessionInfo session = eventDocument.getSessions().stream()
                    .filter(s -> s.getId().equals(sessionId))
                    .findFirst()
                    .orElse(null);

                if (session == null) {
                    return Mono.empty();
                }

                // Fetch public discounts for the event
                return eventReadRepository.findPublicDiscountsByEvent(eventDocument.getId())
                    .collectList()
                    .map(discounts -> eventMapper.mapToSessionInfoDTO(session, discounts));
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
                .map(eventMapper::mapToDiscountDetailsDTO);
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
                .map(eventMapper::mapToDiscountDetailsDTO)
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
                .map(eventMapper::mapToDiscountDetailsDTO)
                .doOnSuccess(dto -> {
                    if (dto != null) log.info("Discount '{}' found for event {} and session {}", code, eventId, sessionId);
                    else log.info("Discount '{}' not found or not applicable for session {}", code, sessionId);
                });
    }
}
