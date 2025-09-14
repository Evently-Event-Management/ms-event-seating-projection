package com.ticketly.mseventseatingprojection.repository;

import com.ticketly.mseventseatingprojection.dto.internal.SeatValidationResponse;
import com.ticketly.mseventseatingprojection.model.EventDocument;
import com.ticketly.mseventseatingprojection.model.ReadModelSeatStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface SeatRepository {
    /**
     * Validates the availability of the specified seats for a given event and session.
     *
     * @param eventId   The ID of the event.
     * @param sessionId The ID of the session.
     * @param SeatIds   The list of seat IDs to validate.
     * @return A Mono emitting a SeatValidationResponse indicating which seats are unavailable.
     */
    Mono<SeatValidationResponse> validateSeatsAvailability(String eventId, String sessionId, List<String> SeatIds);

    /**
     * Finds and returns detailed information for the specified seats in a session.
     *
     * @param eventId   The ID of the event.
     * @param sessionId The ID of the session.
     * @param SeatIds   The list of seat IDs to retrieve details for.
     * @return A Flux emitting EventDocument.SeatInfo for each requested seat.
     */
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
