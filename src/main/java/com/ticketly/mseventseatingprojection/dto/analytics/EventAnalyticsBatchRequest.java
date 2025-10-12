package com.ticketly.mseventseatingprojection.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * DTO for batch ownership verification requests
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EventAnalyticsBatchRequest {
    private List<String> eventIds;
}

