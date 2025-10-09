package com.ticketly.mseventseatingprojection.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "event_views")
@CompoundIndex(name = "event_date_idx", def = "{'eventId' : 1, 'trackingBucket.date': 1}")
public class EventTrackingDocument {
    @Id
    private String id;
    private String eventId;
    private TrackingBucket trackingBucket;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrackingBucket {
        private LocalDate date; // Use LocalDate for the date bucket
        private long mobileViews;
        private long desktopViews;
        private long tabletViews;
        private long otherViews;
        private long orderCount;
    }
}