package com.ticketly.mseventseatingprojection.dto.read;

import dto.projection.discount.DiscountParametersDTO;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class DiscountDetailsDTO {
    private String id;
    private String code;
    private DiscountParametersDTO parameters;
    private boolean isActive;
    private boolean isPublic;
    private Instant activeFrom;
    private Instant expiresAt;
}