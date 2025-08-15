package com.ticketly.mseventseatingprojection.model;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexed;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
@Document(collection = "events")
public class EventDocument {

    @Id
    private String id;

    @TextIndexed
    private String title;
    private String status;

    @TextIndexed
    private String description;
    private String overview;
    private List<String> coverPhotos;

    private OrganizationInfo organization;
    private CategoryInfo category;
    private List<TierInfo> tiers;
    private List<SessionInfo> sessions;

    // --- Embedded Sub-documents ---

    @Data
    @Builder
    public static class OrganizationInfo {
        private String id;
        private String name;
        private String logoUrl;
    }

    @Data
    @Builder
    public static class CategoryInfo {
        private String id;
        private String name;
        private String parentName;
    }

    @Data
    @Builder
    public static class TierInfo {
        private String id;
        private String name;
        private BigDecimal price;
        private String color;
    }

    @Data
    @Builder
    public static class SessionInfo {
        private String id;
        private OffsetDateTime startTime;
        private OffsetDateTime endTime;
        private String status;
        private String sessionType;
        private VenueDetailsInfo venueDetails;
        // ✅ ADDED: The complete, denormalized seating map for this session
        private SessionSeatingMapInfo layoutData;
    }

    @Data
    @Builder
    public static class VenueDetailsInfo {
        private String name;
        private String address;
        private String onlineLink;
        @GeoSpatialIndexed
        private GeoJsonPoint location;
    }

    @Data
    @Builder
    public static class GeoJsonPoint {
        private final String type = "Point";
        private double[] coordinates = new double[2]; // [longitude, latitude]
    }

    // ✅ NEW: Nested classes for the denormalized seating map
    @Data
    @Builder
    public static class SessionSeatingMapInfo {
        private String name;
        private LayoutInfo layout;
    }

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
        private TierInfo tier; // The full, embedded tier details
    }

    @Data
    @Builder
    public static class PositionInfo {
        private Double x;
        private Double y;
    }
}
