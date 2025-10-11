package com.ticketly.mseventseatingprojection.service;

import com.ticketly.mseventseatingprojection.dto.internal.SeatDetailsResponse;
import com.ticketly.mseventseatingprojection.dto.internal.SeatInfoRequest;
import com.ticketly.mseventseatingprojection.dto.internal.SeatValidationResponse;
import com.ticketly.mseventseatingprojection.model.EventDocument;
import com.ticketly.mseventseatingprojection.model.ReadModelSeatStatus;
import com.ticketly.mseventseatingprojection.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class SeatService {
    private final SeatRepository seatRepository;

    /**
     * Validates the availability of the specified seats for a session.
     *
     * @param sessionId The ID of the session.
     * @param request   The seat info request containing seat IDs and event ID.
     * @return Mono emitting SeatValidationResponse.
     */
    public Mono<SeatValidationResponse> validateSeatsAvailability(String sessionId, SeatInfoRequest request) {
        log.debug("validateSeatsAvailability called for sessionId={}, seatIds={}", sessionId, request.getSeat_ids());
        return seatRepository.validateSeatsAvailability(String.valueOf(request.getEvent_id()), sessionId, request.getSeat_ids());
    }

    /**
     * Retrieves detailed information for the specified seats in a session.
     *
     * @param sessionId The ID of the session.
     * @param request   The seat info request containing seat IDs and event ID.
     * @return Flux emitting SeatDetailsResponse for each seat.
     */
    public Flux<SeatDetailsResponse> getSeatDetails(String sessionId, SeatInfoRequest request) {
        log.debug("getSeatDetails called for sessionId={}, seatIdsCount={}", sessionId, request.getSeat_ids().size());
        return seatRepository.findSeatDetails(String.valueOf(request.getEvent_id()), sessionId, request.getSeat_ids())
                .flatMap(this::toSeatDetailsResponse);
    }

    private Flux<SeatDetailsResponse> toSeatDetailsResponse(EventDocument.SeatInfo response) {
        return Flux.just(SeatDetailsResponse.builder()
                .seatId(UUID.fromString(response.getId()))
                .label(response.getLabel())
                .tier(SeatDetailsResponse.TierInfo.builder()
                        .id(UUID.fromString(response.getTier().getId()))
                        .name(response.getTier().getName())
                        .price(response.getTier().getPrice())
                        .color(response.getTier().getColor())
                        .build())
                .build());
    }

    /**
     * Updates the status of the specified seats for a given session.
     *
     * @param sessionId The ID of the session.
     * @param seatIds   The list of seat IDs to update.
     * @param newStatus The new status to set for the seats.
     * @return Mono signaling completion.
     */
    public Mono<Boolean> updateSeatStatus(String sessionId, List<String> seatIds, ReadModelSeatStatus newStatus) {
        if (seatIds == null || seatIds.isEmpty()) {
            return Mono.just(false);
        }

        log.info("Attempting to update {} seats to status {} in session {}", seatIds.size(), newStatus, sessionId);

        // 1. Check if any of the seats are already BOOKED
        return seatRepository.areAnySeatsBooked(sessionId, seatIds)
                .flatMap(anyBooked -> {
                    // 2. Enforce the business rule
                    if (anyBooked) {
                        log.warn("Update failed: Attempted to change the status of an already BOOKED seat in session {}", sessionId);
                        // Return false indicating a business rule violation
                        return Mono.just(false);
                    }

                    // 3. If the rule passes, proceed with the update
                    log.info("Validation passed. Proceeding with update for {} seats in session {}", seatIds.size(), sessionId);
                    return seatRepository.updateSeatStatuses(sessionId, seatIds, newStatus)
                            .thenReturn(true)
                            .onErrorReturn(false);
                });
    }

    /**
     * Updates the status of the specified seats for a given session using UUIDs.
     *
     * @param sessionId The UUID of the session.
     * @param seatIds   The list of seat UUIDs to update.
     * @param newStatus The new status to set for the seats.
     * @return Mono signaling completion.
     */
    public Mono<Boolean> updateSeatStatus(UUID sessionId, List<UUID> seatIds, ReadModelSeatStatus newStatus) {
        List<String> seatIdStrings = seatIds.stream()
                .map(UUID::toString)
                .collect(Collectors.toList());
        return updateSeatStatus(sessionId.toString(), seatIdStrings, newStatus);
    }
}
