package com.ticketly.mseventseatingprojection.dto.analytics;

import lombok.Builder;
import lombok.Data;

/**
 * DTO representing occupancy analytics for a specific venue block
 */
@Data
@Builder
public class BlockOccupancyDTO {
    private String blockId;
    private String blockName;
    private String blockType;
    private Integer totalCapacity;
    private Integer seatsSold;
    private Double occupancyPercentage;
}
