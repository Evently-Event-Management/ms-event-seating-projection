package com.ticketly.mseventseatingprojection.dto.read;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
public class EventThumbnailDTO {
    private String id;
    private String title;
    private String coverPhotoUrl; // Only the first one
    private String organizationName;
    private String organizationId;
    private String categoryName;
    private EarliestSessionInfo earliestSession;
    private BigDecimal startingPrice;
    private List<DiscountThumbnailDTO> discounts;

    @Data
    @Builder
    public static class EarliestSessionInfo {
        private Instant startTime;
        private String venueName;
        private String city; // Extracted for display
    }
}
