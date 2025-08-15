package com.ticketly.mseventseatingprojection.dto;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.UUID;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CoverPhotoChangePayload {
    // This field will be in the 'before' part of a delete event
    private UUID id;

    @JsonProperty("event_id")
    private UUID eventId;

    @JsonProperty("photo_url")
    private String photoUrl; // This is the S3 key
}