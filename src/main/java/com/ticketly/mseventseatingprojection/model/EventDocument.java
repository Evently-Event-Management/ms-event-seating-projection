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
@Document(collection = "events") // This maps the class to the "events" collection in MongoDB
public class EventDocument {

    @Id
    private String id; // The event UUID from PostgreSQL will be the _id here

    @TextIndexed // Enable full-text search on the title
    private String title;
    private String status;

    @TextIndexed // Also enable search on the description
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
    }

    @Data
    @Builder
    public static class VenueDetailsInfo {
        private String name;
        private String address;
        private String onlineLink;

        // For geospatial queries ("events near me")
        @GeoSpatialIndexed
        private GeoJsonPoint location;
    }

    // Standard GeoJSON point structure for MongoDB
    @Data
    @Builder
    public static class GeoJsonPoint {
        private final String type = "Point";
        private double[] coordinates = new double[2]; // [longitude, latitude]
    }
}
