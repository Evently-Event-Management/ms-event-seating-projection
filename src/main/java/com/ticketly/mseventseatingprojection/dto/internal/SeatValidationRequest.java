package com.ticketly.mseventseatingprojection.dto.internal;

import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * Request DTO sent from the Order Service to the Query Service
 * to check if a list of seats are available for a specific session.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatValidationRequest {
    @NotEmpty
    private List<String> seatIds;
}


