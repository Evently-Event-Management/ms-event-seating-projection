package com.ticketly.mseventseatingprojection.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.UUID;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DiscountJoinTablePayload {
    // We only need the discount_id to know which discount's relationships have changed.
    @JsonProperty("discount_id")
    private UUID discountId;
}