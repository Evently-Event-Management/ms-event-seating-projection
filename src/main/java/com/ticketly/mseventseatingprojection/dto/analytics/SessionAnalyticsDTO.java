package com.ticketly.mseventseatingprojection.dto.analytics;

import com.ticketly.mseventseatingprojection.model.ReadModelSeatStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * DTO representing detailed analytics for a specific event session
 * Extends SessionSummaryDTO to add detailed analytics information
 */
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
public class SessionAnalyticsDTO extends SessionSummaryDTO {
    // Time-based insights
    private Duration timeUntilStart;
    private Duration salesWindowDuration;

    // Sales breakdown
    private List<TierSalesDTO> salesByTier;

    // Seat status breakdown
    private Map<ReadModelSeatStatus, Integer> seatStatusBreakdown;

    // Block occupancy breakdown
    private List<BlockOccupancyDTO> occupancyByBlock;
}
