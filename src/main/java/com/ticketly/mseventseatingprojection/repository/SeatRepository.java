package com.ticketly.mseventseatingprojection.repository;

import com.ticketly.mseventseatingprojection.dto.internal.SeatValidationResponse;
import com.ticketly.mseventseatingprojection.model.EventDocument;
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
}
