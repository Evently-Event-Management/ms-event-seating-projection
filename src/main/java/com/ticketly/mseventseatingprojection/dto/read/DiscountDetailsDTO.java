package com.ticketly.mseventseatingprojection.dto.read;

import dto.projection.discount.DiscountParametersDTO;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

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
    private Integer maxUsage;
    private Integer currentUsage;
    private List<TierInfo> applicableTiers;


    @Data
    @Builder
    public static class TierInfo {
        private String id;
        private String name;
        private BigDecimal price;
        private String color;
    }
}