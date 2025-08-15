package com.ticketly.mseventseatingprojection.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.UUID;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CategoryChangePayload {
    private UUID id;
    private String name;
    @JsonProperty("parent_id") // Match the snake_case from the database
    private UUID parentId;
}