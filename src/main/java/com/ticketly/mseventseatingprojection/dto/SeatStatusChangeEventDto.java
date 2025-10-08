package com.ticketly.mseventseatingprojection.dto;

import com.ticketly.mseventseatingprojection.model.ReadModelSeatStatus;

import java.util.List;
import java.util.UUID;

// Using a record for an immutable, concise DTO
public record SeatStatusChangeEventDto(
        UUID session_id,
        List<UUID> seat_ids,
        ReadModelSeatStatus status
) {
}