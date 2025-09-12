package com.ticketly.mseventseatingprojection.service;

import com.ticketly.mseventseatingprojection.model.ReadModelSeatStatus;
import com.ticketly.mseventseatingprojection.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SeatStatusService {

    private final SeatRepository seatRepository;


    public Mono<Void> updateSeatStatus(String sessionId, List<String> seatIds, ReadModelSeatStatus newStatus) {
        if (seatIds == null || seatIds.isEmpty()) {
            return Mono.empty();
        }

        log.info("Attempting to update {} seats to status {} in session {}", seatIds.size(), newStatus, sessionId);

        // 1. Check if any of the seats are already BOOKED
        return seatRepository.areAnySeatsBooked(sessionId, seatIds)
                .flatMap(anyBooked -> {
                    // 2. Enforce the business rule
                    if (anyBooked) {
                        log.warn("Update failed: Attempted to change the status of an already BOOKED seat in session {}", sessionId);
                        // Return an error indicating a business rule violation
                        return Mono.error(new IllegalStateException("Cannot change the status of a seat that is already BOOKED."));
                    }

                    // 3. If the rule passes, proceed with the update
                    log.info("Validation passed. Proceeding with update for {} seats in session {}", seatIds.size(), sessionId);
                    return seatRepository.updateSeatStatuses(sessionId, seatIds, newStatus);
                }).then();
    }

    public Mono<Void> updateSeatStatus(UUID sessionId, List<UUID> seatIds, ReadModelSeatStatus newStatus) {
        List<String> seatIdStrings = seatIds.stream()
                .map(UUID::toString)
                .collect(Collectors.toList());
        return updateSeatStatus(sessionId.toString(), seatIdStrings, newStatus);
    }
}