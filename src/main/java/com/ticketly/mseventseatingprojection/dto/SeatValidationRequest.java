package com.ticketly.mseventseatingprojection.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.UUID;

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


