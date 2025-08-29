package com.ticketly.mseventseatingprojection.dto;

import java.util.List;
import java.util.UUID;

// Using a record for an immutable, concise DTO
public record SeatStatusChangeEventDto(
        UUID sessionId,
        List<UUID> seatIds
) {
}