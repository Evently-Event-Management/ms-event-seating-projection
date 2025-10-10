package com.ticketly.mseventseatingprojection.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.UUID;

@Data
public class TierChangePayload {
    private UUID id;
    private String name;
    private String color;
    private Object price;
    
    @JsonProperty("event_id")
    private UUID eventId;
}
