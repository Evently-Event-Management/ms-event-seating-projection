package com.ticketly.mseventseatingprojection.dto.analytics.raw;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import model.SessionStatus;

/**
 * DTO for session status counts from aggregation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatStatusCountDTO {
    private SessionStatus status;
    private Integer count;
}