package com.ticketly.mseventseatingprojection.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EventViewsDTO {
    private int totalViews;
    private List<TimeSeriesData> viewsTimeSeries;
    private List<TrafficSource> trafficSources;
    private List<AudienceGeo> audienceGeography;
    private List<DeviceBreakdown> deviceBreakdown;
    
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TimeSeriesData {
        private String date;
        private int views;
    }
    
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TrafficSource {
        private String source;
        private String medium;
        private int views;
    }
    
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AudienceGeo {
        private String location;
        private int views;
    }
    
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DeviceBreakdown {
        private String device;
        private int views;
    }
}