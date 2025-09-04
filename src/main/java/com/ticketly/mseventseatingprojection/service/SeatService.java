package com.ticketly.mseventseatingprojection.service;

import com.ticketly.mseventseatingprojection.dto.internal.SeatDetailsResponse;
import com.ticketly.mseventseatingprojection.dto.internal.SeatInfoRequest;
import com.ticketly.mseventseatingprojection.dto.internal.SeatValidationResponse;
import com.ticketly.mseventseatingprojection.model.EventDocument;
import com.ticketly.mseventseatingprojection.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class SeatService {
    private final SeatRepository seatRepository;

    public Mono<SeatValidationResponse> validateSeatsAvailability(String sessionId, SeatInfoRequest request) {
        log.debug("validateSeatsAvailability called for sessionId={}, seatIds={}", sessionId, request.getSeat_ids());
        return seatRepository.validateSeatsAvailability(String.valueOf(request.getEvent_id()), sessionId, request.getSeat_ids());
    }

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
}
