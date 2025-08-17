package com.ticketly.mseventseatingprojection.controller;

import com.ticketly.mseventseatingprojection.dto.SeatValidationRequest;
import com.ticketly.mseventseatingprojection.dto.SeatValidationResponse;
import com.ticketly.mseventseatingprojection.service.EventQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

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
    // In production, you would secure this with @PreAuthorize("hasAuthority('SCOPE_internal-api')")
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
}
