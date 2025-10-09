package com.ticketly.mseventseatingprojection.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventViewsStatsDTO {
    private String eventId;
    private long totalMobileViews;
    private long totalDesktopViews;
    private long totalTabletViews;
    private long totalOtherViews;
    private long totalOrders;
    private LocalDate fromDate;
    private LocalDate toDate;
    private List<DailyStats> dailyStats;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyStats {
        private LocalDate date;
        private long mobileViews;
        private long desktopViews;
        private long tabletViews;
        private long otherViews;
        private long orderCount;
    }
}