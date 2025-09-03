package com.ticketly.mseventseatingprojection.dto.analytics;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * DTO representing overall analytics for an event across all sessions
 */
@Data
@Builder
public class EventAnalyticsDTO {
    private String eventId;
    private String eventTitle;

    // Revenue metrics
    private BigDecimal totalRevenue;
    private BigDecimal averageRevenuePerTicket;

    // Capacity metrics
    private Integer totalTicketsSold;
    private Integer totalEventCapacity;
    private Double overallSellOutPercentage;

    // Session status overview
    private Map<String, Integer> sessionStatusBreakdown;

    // Tier sales breakdown
    private List<TierSalesDTO> salesByTier;
}
