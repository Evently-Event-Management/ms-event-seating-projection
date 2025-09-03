package com.ticketly.mseventseatingprojection.dto.analytics;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * DTO representing sales analytics for a specific ticket tier
 */
@Data
@Builder
public class TierSalesDTO {
    private String tierId;
    private String tierName;
    private String tierColor;
    private Integer ticketsSold;
    private BigDecimal totalRevenue;
    private Double percentageOfTotalSales;
}
