package com.ticketly.mseventseatingprojection.dto.analytics.raw;


import com.ticketly.mseventseatingprojection.model.ReadModelSeatStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for session status counts from aggregation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatStatusCountDTO {
    private ReadModelSeatStatus status;
    private Integer count;
}