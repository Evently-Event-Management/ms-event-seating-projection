package com.ticketly.mseventseatingprojection.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import model.SessionStatus;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * DTO representing basic analytics for an event session (summary view)
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SessionSummaryDTO {
    private String sessionId;
    private String eventId;
    private String eventTitle;
    private Instant startTime;
    private Instant endTime;
    private BigDecimal sessionRevenue;
    private Integer ticketsSold;
    private SessionStatus sessionStatus;
    private Integer sessionCapacity;
    private Double sellOutPercentage;
}
