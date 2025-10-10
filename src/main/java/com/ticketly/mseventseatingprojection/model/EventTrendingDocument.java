package com.ticketly.mseventseatingprojection.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@Document(collection = "event_trending_scores")
@AllArgsConstructor
@NoArgsConstructor
public class EventTrendingDocument {

    @Id
    private String id;
    
    @Indexed
    private String eventId;
    
    private double trendingScore;
    
    private int viewCount;
    
    private int purchaseCount;
    
    private int reservationCount;
    
    @Indexed
    private Instant lastCalculated;
    
    @Indexed
    private Instant lastUpdated;
}