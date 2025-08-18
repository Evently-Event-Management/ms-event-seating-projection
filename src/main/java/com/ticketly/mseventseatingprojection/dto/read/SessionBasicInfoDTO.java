package com.ticketly.mseventseatingprojection.dto.read;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;

import java.time.Instant;

@Data
@Builder
public class SessionBasicInfoDTO {
    private String id;
    private String eventId;
    private String status;
    private Instant startTime;
    private Instant endTime;
    private VenueInfo venue;

    @Data
    @Builder
    public static class VenueInfo {
        private String name;
        private String address;
        private String onlineLink;
        private GeoJsonPoint location;
    }
}
