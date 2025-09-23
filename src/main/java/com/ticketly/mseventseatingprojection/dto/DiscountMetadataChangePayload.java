package com.ticketly.mseventseatingprojection.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DiscountMetadataChangePayload {
    private UUID id;

    @JsonProperty("event_id")
    private UUID eventId;

    private String code;

    // Capture parameters as raw JSON to pass through to MongoDB
    private JsonNode parameters;

    @JsonProperty("max_usage")
    private Integer maxUsage;

    @JsonProperty("current_usage")
    private int currentUsage;

    @JsonProperty("is_active")
    private boolean isActive;

    @JsonProperty("is_public")
    private boolean isPublic;

    @JsonProperty("active_from")
    private OffsetDateTime activeFrom;

    @JsonProperty("expires_at")
    private OffsetDateTime expiresAt;
}