package com.ticketly.mseventseatingprojection.service;

import com.ticketly.mseventseatingprojection.dto.internal.SeatDetailsResponse;
import com.ticketly.mseventseatingprojection.dto.internal.SeatInfoRequest;
import com.ticketly.mseventseatingprojection.dto.internal.SeatValidationResponse;
import com.ticketly.mseventseatingprojection.model.EventDocument;
import com.ticketly.mseventseatingprojection.model.ReadModelSeatStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Service
@Slf4j
@RequiredArgsConstructor
public class SeatValidationService {


    private final EventReadRepository eventReadRepository;

    /**
     * Validates if a list of seats for a given session are all available.
     *
     * @param sessionId The ID of the session.
     * @param request   The request containing the list of seat IDs to check.
     * @return A Mono emitting a response indicating availability.
     */
    public Mono<SeatValidationResponse> validateSeatsAvailability(String sessionId, SeatInfoRequest request) {
        log.debug("validateSeatsAvailability called for sessionId={}, seatIds={}", sessionId, request.getSeatIds());

        return eventReadRepository.findEventBySessionId(sessionId)
                .map(eventDocument -> {
                    // Find the specific session within the event document
                    EventDocument.SessionInfo session = eventDocument.getSessions().stream()
                            .filter(s -> s.getId().equals(sessionId))
                            .findFirst()
                            .orElse(null);

                    if (session == null || session.getLayoutData() == null) {
                        // If session or layout doesn't exist, seats are considered unavailable
                        log.info("validateSeatsAvailability outcome: session/layout missing for sessionId={}", sessionId);
                        return SeatValidationResponse.builder()
                                .allAvailable(false)
                                .unavailableSeats(request.getSeatIds())
                                .build();
                    }

                    log.debug("Session found for sessionId={}, computing available seats", sessionId);

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

                    log.info("validateSeatsAvailability outcome for sessionId={}: allAvailable={}, unavailableCount={}",
                            sessionId, unavailableSeats.isEmpty(), unavailableSeats.size());

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
        log.debug("getSeatDetails called for sessionId={}, seatIdsCount={}", sessionId, seatIds != null ? seatIds.size() : 0);

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
                        log.info("getSeatDetails outcome: session/layout missing for sessionId={}", sessionId);
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

                    log.info("getSeatDetails outcome for sessionId={}: matchedSeatsCount={}", sessionId, matchedSeats.size());

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
}
