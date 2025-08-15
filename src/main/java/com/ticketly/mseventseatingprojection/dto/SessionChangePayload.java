package com.ticketly.mseventseatingprojection.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.UUID;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SessionChangePayload {
    private UUID id;
    @JsonProperty("event_id")
    private UUID eventId;
    private String status;
}