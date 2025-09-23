package com.ticketly.mseventseatingprojection.model;

import lombok.Builder;
import lombok.Data;
import model.EventStatus;
import model.SessionStatus;
import model.SessionType;
import model.DiscountType;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.index.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@Document(collection = "events")
public class EventDocument {

    @Id
    private String id;

    @TextIndexed(weight = 5.0f)
    @Indexed
    private String title;
    private EventStatus status;

    @TextIndexed
    private String description;
    private String overview;
    private List<String> coverPhotos;

    private OrganizationInfo organization;
    private CategoryInfo category;
    private List<TierInfo> tiers;
    private List<SessionInfo> sessions;
    private List<DiscountInfo> discounts;

    // --- Embedded Sub-documents ---

    @Data
    @Builder
    public static class DiscountInfo {
        private String id;
        private String code;
        private DiscountParametersInfo parameters;
        private Integer maxUsage;
        private Integer currentUsage;
        private Instant activeFrom;
        private Instant expiresAt;
        private boolean isActive;
        private boolean isPublic;
        private List<String> applicableTierIds;
        private List<String> applicableSessionIds;
    }

    @Data
    @Builder
    public static class DiscountParametersInfo {
        private DiscountType type;
        private BigDecimal percentage;
        private BigDecimal amount;
        private String currency;
        private Integer buyQuantity;
        private Integer getQuantity;
    }

    @Data
    @Builder
    public static class OrganizationInfo {
        private String id;
        private String name;
        private String logoUrl;
        private String userId;
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
        @Indexed
        private String id;
        private Instant startTime;
        private Instant endTime;
        private Instant salesStartTime;
        private SessionStatus status;
        private SessionType sessionType;
        private VenueDetailsInfo venueDetails;
        private SessionSeatingMapInfo layoutData;
    }

    @Data
    @Builder
    public static class VenueDetailsInfo {
        private String name;
        private String address;
        private String onlineLink;
        @GeoSpatialIndexed(type = GeoSpatialIndexType.GEO_2DSPHERE)
        private GeoJsonPoint location;
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
        private ReadModelSeatStatus status;
        private TierInfo tier; // The full, embedded tier details
    }

    @Data
    @Builder
    public static class PositionInfo {
        private Double x;
        private Double y;
    }
}
