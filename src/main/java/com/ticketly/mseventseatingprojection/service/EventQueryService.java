package com.ticketly.mseventseatingprojection.service;

import com.ticketly.mseventseatingprojection.dto.SeatValidationRequest;
import com.ticketly.mseventseatingprojection.dto.SeatValidationResponse;
import com.ticketly.mseventseatingprojection.dto.read.EventThumbnailDTO;
import com.ticketly.mseventseatingprojection.model.EventDocument;
import com.ticketly.mseventseatingprojection.model.ReadModelSeatStatus;
import com.ticketly.mseventseatingprojection.repository.EventReadRepositoryCustomImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class EventQueryService {

    private final EventReadRepositoryCustomImpl eventReadRepository;

    public Mono<Page<EventThumbnailDTO>> searchEvents(
            String searchTerm, String categoryId, Double longitude, Double latitude,
            Integer radiusKm, Instant dateFrom, Instant dateTo,
            BigDecimal priceMin, BigDecimal priceMax, Pageable pageable
    ) {
        return eventReadRepository.searchEvents(
                searchTerm, categoryId, longitude, latitude, radiusKm,
                dateFrom, dateTo, priceMin, priceMax, pageable
        ).map(eventPage -> eventPage.map(this::mapToThumbnailDTO));
    }

    /**
     * Validates if a list of seats for a given session are all available.
     *
     * @param sessionId The ID of the session.
     * @param request   The request containing the list of seat IDs to check.
     * @return A Mono emitting a response indicating availability.
     */
    public Mono<SeatValidationResponse> validateSeatsAvailability(String sessionId, SeatValidationRequest request) {
        return eventReadRepository.findEventBySessionId(sessionId)
                .map(eventDocument -> {
                    // Find the specific session within the event document
                    EventDocument.SessionInfo session = eventDocument.getSessions().stream()
                            .filter(s -> s.getId().equals(sessionId))
                            .findFirst()
                            .orElse(null);

                    if (session == null || session.getLayoutData() == null) {
                        // If session or layout doesn't exist, seats are considered unavailable
                        return SeatValidationResponse.builder()
                                .allAvailable(false)
                                .unavailableSeats(request.getSeatIds())
                                .build();
                    }

                    // Create a set of all available seat IDs in the session for fast lookup
                    var availableSeats = session.getLayoutData().getLayout().getBlocks().stream()
                            .flatMap(block -> block.getRows() != null ? block.getRows().stream() : block.getSeats().stream())
                            .flatMap(rowOrSeat -> rowOrSeat instanceof EventDocument.RowInfo ? ((EventDocument.RowInfo) rowOrSeat).getSeats().stream() : Stream.of((EventDocument.SeatInfo) rowOrSeat))
                            .filter(seat -> ReadModelSeatStatus.AVAILABLE.equals(seat.getStatus()))
                            .map(EventDocument.SeatInfo::getId)
                            .collect(Collectors.toSet());

                    // Find which of the requested seats are NOT in the available set
                    List<String> unavailableSeats = request.getSeatIds().stream()
                            .filter(requestedSeatId -> !availableSeats.contains(requestedSeatId))
                            .collect(Collectors.toList());

                    return SeatValidationResponse.builder()
                            .allAvailable(unavailableSeats.isEmpty())
                            .unavailableSeats(unavailableSeats)
                            .build();
                })
                .defaultIfEmpty(SeatValidationResponse.builder()
                        .allAvailable(false)
                        .unavailableSeats(request.getSeatIds())
                        .build());
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
}
