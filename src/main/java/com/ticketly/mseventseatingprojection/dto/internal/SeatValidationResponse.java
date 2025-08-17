package com.ticketly.mseventseatingprojection.dto.internal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO sent back to the Order Service.
 * Contains the status and a list of any unavailable seats.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatValidationResponse {
    private boolean allAvailable;
    private List<String> unavailableSeats;
}