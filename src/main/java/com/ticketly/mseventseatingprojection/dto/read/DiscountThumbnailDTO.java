package com.ticketly.mseventseatingprojection.dto.read;

import dto.projection.discount.DiscountParametersDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiscountThumbnailDTO {
    private DiscountParametersDTO parameters;
    private Instant expiresAt;
    private Integer maxUsage;
    private Integer currentUsage;
}