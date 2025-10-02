package com.ticketly.mseventseatingprojection.dto;

import com.ticketly.mseventseatingprojection.dto.read.DiscountThumbnailDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import model.SessionStatus;
import model.SessionType;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SessionInfoDTO {
    private String id;
    private Instant startTime;
    private Instant endTime;
    private Instant salesStartTime;
    private SessionStatus status;
    private SessionType sessionType;
    private VenueDetailsInfo venueDetails;
    private List<DiscountThumbnailDTO> discounts;


    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class VenueDetailsInfo {
        private String name;
        private String address;
        private String onlineLink;
        private GeoJsonPoint location;
    }
}