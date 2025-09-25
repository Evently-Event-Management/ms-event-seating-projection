package com.ticketly.mseventseatingprojection.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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
    private Long activeFromMicros;

    @JsonProperty("expires_at")
    private Long expiresAtMicros;

    public OffsetDateTime getActiveFrom() {
        return activeFromMicros == null ? null :
                Instant.ofEpochMilli(activeFromMicros / 1000).atOffset(ZoneOffset.UTC);
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAtMicros == null ? null :
                Instant.ofEpochMilli(expiresAtMicros / 1000).atOffset(ZoneOffset.UTC);
    }
}