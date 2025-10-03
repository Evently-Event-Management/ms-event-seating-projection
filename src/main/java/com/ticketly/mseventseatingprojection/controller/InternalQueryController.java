package com.ticketly.mseventseatingprojection.controller;

import com.ticketly.mseventseatingprojection.dto.internal.*;
import com.ticketly.mseventseatingprojection.service.EventQueryService;
import com.ticketly.mseventseatingprojection.service.SeatService;
import dto.CreateOrderRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/internal/v1") // Dedicated path for internal M2M calls
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('SCOPE_internal-api')")
@Slf4j
public class InternalQueryController {

    private final SeatService seatService;
    private final EventQueryService eventQueryService;

    /**
     * Secure M2M endpoint for the Ticket/Order Service to pre-validate seat availability.
     *
     * @param sessionId The ID of the session to check.
     * @param request   The request containing the seat IDs.
     * @return A 200 OK if all seats are available, or a 409 Conflict if any are not.
     */
    @PostMapping("/sessions/{sessionId}/seats/validate")
    public Mono<ResponseEntity<SeatValidationResponse>> validateSeats(
            @PathVariable String sessionId,
            @Valid @RequestBody SeatInfoRequest request) {

        log.info("validateSeats requested for sessionId={}, seatIdsCount={}. Will validate availability.", sessionId, request.getSeat_ids() != null ? request.getSeat_ids().size() : 0);
        log.debug("validateSeats request payload: {}", request);

        return seatService.validateSeatsAvailability(sessionId, request)
                .map(response -> {
                    log.info("validateSeats result for sessionId={}: allAvailable={}", sessionId, response.isAllAvailable());
                    if (response.isAllAvailable()) {
                        return ResponseEntity.ok(response);
                    } else {
                        // 409 Conflict is the appropriate status code to indicate that the
                        // resource state prevents the request from being completed.
                        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
                    }
                });
    }

    /**
     * Secure M2M endpoint for the Ticket/Order Service to get detailed information about seats.
     * This provides authoritative data about each seat, including its tier and price.
     *
     * @param sessionId The ID of the session containing the seats.
     * @param request   The request containing the seat IDs to retrieve details for.
     * @return A list of seat details.
     */
    @PostMapping("/sessions/{sessionId}/seats/details")
    public Flux<SeatDetailsResponse> getSeatDetails(
            @PathVariable String sessionId,
            @Valid @RequestBody SeatInfoRequest request) {

        log.info("getSeatDetails requested for sessionId={}, requestedSeatCount={}. Will fetch seat details.", sessionId, request.getSeat_ids() != null ? request.getSeat_ids().size() : 0);
        log.debug("getSeatDetails request payload: {}", request);

        return seatService.getSeatDetails(sessionId, request);
    }

    /**
     * Validates the details of a potential order before creation.
     * Checks seat availability, event/session status, and discount validity.
     *
     * @param request The validation request containing event, session, seat, and discount IDs.
     * @return A Mono emitting the validated seat and discount details, or a 400 Bad Request if validation fails.
     */
    @PostMapping("/validate-pre-order")
    public Mono<ResponseEntity<PreOrderValidationResponse>> validatePreOrder(
            @RequestBody @Valid CreateOrderRequest request
    ) {
        return eventQueryService.validatePreOrderDetails(request)
                .map(ResponseEntity::ok);
    }
}
