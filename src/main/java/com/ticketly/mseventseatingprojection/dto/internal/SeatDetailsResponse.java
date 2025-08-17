package com.ticketly.mseventseatingprojection.dto.internal;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class SeatDetailsResponse {
    private UUID seatId;
    private String label;
    private TierInfo tier;

    @Data
    @Builder
    public static class TierInfo {
        private UUID id;
        private String name;
        private BigDecimal price;
        private String color;
    }
}