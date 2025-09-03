package com.ticketly.mseventseatingprojection.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for tier-based analytics from aggregation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TierAnalyticsDTO {
    private String tierId;
    private String tierName;
    private String tierColor;
    private BigDecimal totalRevenue;
    private Integer tierCapacity;
    private Integer ticketsSold;
}

