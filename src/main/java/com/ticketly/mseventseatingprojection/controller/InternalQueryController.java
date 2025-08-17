package com.ticketly.mseventseatingprojection.controller;

import com.ticketly.mseventseatingprojection.dto.internal.SeatDetailsRequest;
import com.ticketly.mseventseatingprojection.dto.internal.SeatDetailsResponse;
import com.ticketly.mseventseatingprojection.dto.internal.SeatValidationRequest;
import com.ticketly.mseventseatingprojection.dto.internal.SeatValidationResponse;
import com.ticketly.mseventseatingprojection.service.EventQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/internal/v1/sessions") // Dedicated path for internal M2M calls
@RequiredArgsConstructor
public class InternalQueryController {

    private final EventQueryService eventQueryService;

    /**
     * Secure M2M endpoint for the Ticket/Order Service to pre-validate seat availability.
     *
     * @param sessionId The ID of the session to check.
     * @param request   The request containing the seat IDs.
     * @return A 200 OK if all seats are available, or a 409 Conflict if any are not.
     */
    @PostMapping("/{sessionId}/seats/validate")
    @PreAuthorize("hasAuthority('SCOPE_internal-api')")
    public Mono<ResponseEntity<SeatValidationResponse>> validateSeats(
            @PathVariable String sessionId,
            @Valid @RequestBody SeatValidationRequest request) {

        return eventQueryService.validateSeatsAvailability(sessionId, request)
                .map(response -> {
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
    @PostMapping("/{sessionId}/seats/details")
    @PreAuthorize("hasAuthority('SCOPE_internal-api')")
    public Flux<SeatDetailsResponse> getSeatDetails(
            @PathVariable String sessionId,
            @Valid @RequestBody SeatDetailsRequest request) {

        return eventQueryService.getSeatDetails(sessionId, request.getSeatIds());
    }
}
