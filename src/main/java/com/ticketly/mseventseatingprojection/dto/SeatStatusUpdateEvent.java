package com.ticketly.mseventseatingprojection.dto; // Or your chosen DTO package

import com.ticketly.mseventseatingprojection.model.SeatStatus;
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
    private SeatStatus status;
    private String userId;
    private OffsetDateTime timestamp;
}