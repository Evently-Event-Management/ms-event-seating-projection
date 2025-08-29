package com.ticketly.mseventseatingprojection.dto.read;

import com.ticketly.mseventseatingprojection.model.ReadModelSeatStatus;

import java.util.List;
import java.util.UUID;

public record SeatStatusUpdateDto(
        List<UUID> seatIds,
        ReadModelSeatStatus status
) {
}