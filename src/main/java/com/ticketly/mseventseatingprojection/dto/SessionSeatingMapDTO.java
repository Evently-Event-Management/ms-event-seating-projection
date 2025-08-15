package com.ticketly.mseventseatingprojection.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionSeatingMapDTO {
    private String name;
    private Layout layout;

    @Data
    public static class Layout {
        private List<Block> blocks;
    }

    @Data
    public static class Block {
        private String id;
        private String name;
        private String type;
        private Position position;
        private List<Row> rows;
        private Integer capacity;
        private List<Seat> seats;
        private Integer width;
        private Integer height;
        private Integer soldCount;
        private String tierId;
    }

    @Data
    public static class Row {
        private String id;
        private String label;
        private List<Seat> seats;
    }

    @Data
    public static class Seat {
        private String id;
        private String label;
        private String tierId;
        private String status;
    }

    @Data
    public static class Position {
        private Double x;
        private Double y;
    }
}
