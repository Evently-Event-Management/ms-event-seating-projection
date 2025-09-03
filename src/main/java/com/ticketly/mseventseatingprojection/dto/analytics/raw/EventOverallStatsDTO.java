package com.ticketly.mseventseatingprojection.dto.analytics.raw;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for overall event statistics from aggregation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventOverallStatsDTO {
    private BigDecimal totalRevenue;
    private Integer totalTicketsSold;
    private Integer totalEventCapacity;
}

