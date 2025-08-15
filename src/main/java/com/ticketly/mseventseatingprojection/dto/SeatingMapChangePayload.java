package com.ticketly.mseventseatingprojection.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.UUID;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SeatingMapChangePayload {
    private UUID id;
    @JsonProperty("event_session_id")
    private UUID sessionId;
    @JsonProperty("layout_data")
    private String layoutData; // The raw JSON string
}
