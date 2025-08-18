package com.ticketly.mseventseatingprojection.dto.read;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class EventBasicInfoDTO {
    private String id;
    private String title;
    private String description;
    private String overview;
    private List<String> coverPhotos;
    private OrganizationInfo organization;
    private CategoryInfo category;
    private List<TierInfo> tiers;

    @Data
    @Builder
    public static class OrganizationInfo {
        private String id;
        private String name;
        private String logoUrl;
    }

    @Data
    @Builder
    public static class CategoryInfo {
        private String id;
        private String name;
        private String parentName;
    }

    @Data
    @Builder
    public static class TierInfo {
        private String id;
        private String name;
        private BigDecimal price;
        private String color;
    }
}

