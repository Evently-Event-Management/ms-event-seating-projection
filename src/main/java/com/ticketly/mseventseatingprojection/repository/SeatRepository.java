package com.ticketly.mseventseatingprojection.repository;

import com.ticketly.mseventseatingprojection.dto.internal.SeatValidationResponse;
import com.ticketly.mseventseatingprojection.model.EventDocument;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface SeatRepository {
    Mono<SeatValidationResponse> validateSeatsAvailability(String eventId, String sessionId, List<String> SeatIds);

    Flux<EventDocument.SeatInfo> findSeatDetails(String eventId, String sessionId, List<String> SeatIds);
}
