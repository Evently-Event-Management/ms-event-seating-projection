package com.ticketly.mseventseatingprojection.dto.projection;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Builder
@Data
public class SeatingMapProjectionDTO {
    private String name;
    private LayoutInfo layout;

    @Data
    @Builder
    public static class LayoutInfo {
        private List<BlockInfo> blocks;
    }

    @Data
    @Builder
    public static class BlockInfo {
        private String id;
        private String name;
        private String type;
        private PositionInfo position;
        private List<RowInfo> rows;
        private List<SeatInfo> seats; // For standing capacity blocks
        private Integer capacity;
        private Integer width;
        private Integer height;
    }

    @Data
    @Builder
    public static class RowInfo {
        private String id;
        private String label;
        private List<SeatInfo> seats;
    }

    @Data
    @Builder
    public static class SeatInfo {
        private String id;
        private String label;
        private String status;
        private TierInfo tier;
    }

    @Data
    @Builder
    public static class PositionInfo {
        private Double x;
        private Double y;
    }
}
