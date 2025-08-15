package com.ticketly.mseventseatingprojection.dto.projection;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class TierInfo {
    private UUID id;
    private String name;
    private BigDecimal price;
    private String color;
}
