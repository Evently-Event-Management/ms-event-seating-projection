package com.ticketly.mseventseatingprojection.dto.internal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Request DTO sent from the Order Service to the Event Service
 * to get authoritative details for a list of seats.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeatDetailsRequest {
    private List<UUID> seatIds;
}
