package com.ticketly.mseventseatingprojection.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;

/**
 * DTO for venue details information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VenueDetailsDTO {
    private String name;
    private String address;
    private String onlineLink;
    private GeoJsonPoint location;
}