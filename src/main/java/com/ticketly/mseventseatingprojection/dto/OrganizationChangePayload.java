package com.ticketly.mseventseatingprojection.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.UUID;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrganizationChangePayload {
    private UUID id;
    private String name;
    @JsonProperty("logo_url")
    private String logoUrl;
    private String website;
    @JsonProperty("user_id")
    private String userId;
}