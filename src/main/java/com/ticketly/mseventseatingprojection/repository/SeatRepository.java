package com.ticketly.mseventseatingprojection.repository;

import com.ticketly.mseventseatingprojection.dto.internal.SeatValidationResponse;
import com.ticketly.mseventseatingprojection.model.EventDocument;
import com.ticketly.mseventseatingprojection.model.ReadModelSeatStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface SeatRepository {
    Mono<SeatValidationResponse> validateSeatsAvailability(String eventId, String sessionId, List<String> SeatIds);

    Flux<EventDocument.SeatInfo> findSeatDetails(String eventId, String sessionId, List<String> SeatIds);

    /**
     * Checks if any of the specified seat IDs have a 'BOOKED' status.
     * @return A Mono emitting true if at least one seat is booked, false otherwise.
     */
    Mono<Boolean> areAnySeatsBooked(String sessionId, List<String> seatIds);


    /**
     * Atomically updates the status of multiple seats within a specific session
     * using arrayFilters for efficient nested updates.
     *
     * @param sessionId The ID of the session containing the seats.
     * @param seatIds   The list of seat IDs to update.
     * @param newStatus The new status to set for the seats.
     * @return A Mono that completes when the update is finished, emitting the count of modified documents.
     */
    Mono<Long> updateSeatStatuses(String sessionId, List<String> seatIds, ReadModelSeatStatus newStatus);
}
