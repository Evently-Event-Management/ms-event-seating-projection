package com.ticketly.mseventseatingprojection.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import model.SessionStatus;
import model.SessionType;

import java.time.Instant;

/**
 * Extended DTO for session information that includes the event ID
 * it belongs to.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtendedSessionInfoDTO {
    private String sessionId;
    private String eventId;
    private String eventTitle;
    private Instant startTime;
    private Instant endTime;
    private Instant salesStartTime;
    private SessionStatus status;
    private SessionType sessionType;
    private VenueDetailsDTO venueDetails;
}