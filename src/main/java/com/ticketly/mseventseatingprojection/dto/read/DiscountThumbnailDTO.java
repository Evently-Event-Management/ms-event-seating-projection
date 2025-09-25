package com.ticketly.mseventseatingprojection.dto.read;

import dto.projection.discount.DiscountParametersDTO;
import lombok.Builder;
import lombok.Data;
import java.time.Instant;

@Data
@Builder
public class DiscountThumbnailDTO {
    private DiscountParametersDTO parameters;
    private Instant expiresAt;
    private Integer maxUsage;
    private Integer currentUsage;
}