package com.ticketly.mseventseatingprojection.dto; // Or your chosen DTO package

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatStatusUpdateEvent {
    private UUID sessionId;
    private UUID seatId;
    private String status;
    private String userId;
    private OffsetDateTime timestamp;
}