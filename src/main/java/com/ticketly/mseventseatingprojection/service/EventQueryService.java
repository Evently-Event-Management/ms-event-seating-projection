package com.ticketly.mseventseatingprojection.service;

import com.ticketly.mseventseatingprojection.dto.internal.SeatDetailsResponse;
import com.ticketly.mseventseatingprojection.dto.internal.SeatValidationRequest;
import com.ticketly.mseventseatingprojection.dto.internal.SeatValidationResponse;
import com.ticketly.mseventseatingprojection.dto.read.EventThumbnailDTO;
import com.ticketly.mseventseatingprojection.model.EventDocument;
import com.ticketly.mseventseatingprojection.model.ReadModelSeatStatus;
import com.ticketly.mseventseatingprojection.repository.EventReadRepositoryCustomImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
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

    /**
     * Retrieves detailed information about specific seats in a session.
     *
     * @param sessionId The ID of the session containing the seats.
     * @param seatIds   List of seat IDs to retrieve details for.
     * @return A Flux emitting seat details.
     */
    public Flux<SeatDetailsResponse> getSeatDetails(String sessionId, List<UUID> seatIds) {
        // Create a set of the seat IDs for faster lookup
        Set<String> seatIdStrings = seatIds.stream()
                .map(UUID::toString)
                .collect(Collectors.toSet());

        return eventReadRepository.findEventBySessionId(sessionId)
                .flatMapMany(eventDocument -> {
                    // Find the specific session within the event document
                    Optional<EventDocument.SessionInfo> sessionOpt = eventDocument.getSessions().stream()
                            .filter(s -> s.getId().equals(sessionId))
                            .findFirst();

                    if (sessionOpt.isEmpty() || sessionOpt.get().getLayoutData() == null) {
                        return Flux.empty(); // Session or layout doesn't exist
                    }

                    EventDocument.SessionInfo session = sessionOpt.get();

                    // Find all seats in the session's layout that match the requested IDs
                    List<EventDocument.SeatInfo> matchedSeats = session.getLayoutData().getLayout().getBlocks().stream()
                            .flatMap(block -> {
                                Stream<EventDocument.SeatInfo> rowSeats = Stream.empty();
                                Stream<EventDocument.SeatInfo> directSeats = Stream.empty();

                                // Get seats from rows if rows exist
                                if (block.getRows() != null && !block.getRows().isEmpty()) {
                                    rowSeats = block.getRows().stream()
                                            .flatMap(row -> row.getSeats().stream());
                                }

                                // Get direct seats if they exist
                                if (block.getSeats() != null && !block.getSeats().isEmpty()) {
                                    directSeats = block.getSeats().stream();
                                }

                                // Combine both streams
                                return Stream.concat(rowSeats, directSeats);
                            })
                            .filter(seat -> seatIdStrings.contains(seat.getId()))
                            .collect(Collectors.toList());

                    // Map the matched seats to SeatDetailsResponse objects
                    return Flux.fromIterable(matchedSeats)
                            .map(this::mapToSeatDetailsResponse);
                });
    }

    /**
     * Maps a SeatInfo entity to a SeatDetailsResponse DTO.
     *
     * @param seatInfo The seat entity to map.
     * @return The mapped seat details response.
     */
    private SeatDetailsResponse mapToSeatDetailsResponse(EventDocument.SeatInfo seatInfo) {
        SeatDetailsResponse.TierInfo tierInfo = null;

        if (seatInfo.getTier() != null) {
            tierInfo = SeatDetailsResponse.TierInfo.builder()
                    .id(UUID.fromString(seatInfo.getTier().getId()))
                    .name(seatInfo.getTier().getName())
                    .price(seatInfo.getTier().getPrice())
                    .color(seatInfo.getTier().getColor())
                    .build();
        }

        return SeatDetailsResponse.builder()
                .seatId(UUID.fromString(seatInfo.getId()))
                .label(seatInfo.getLabel())
                .tier(tierInfo)
                .build();
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
