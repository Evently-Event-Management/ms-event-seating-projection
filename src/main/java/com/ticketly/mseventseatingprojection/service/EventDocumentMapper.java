package com.ticketly.mseventseatingprojection.service;


import com.ticketly.mseventseatingprojection.dto.SessionSeatingMapDTO;
import com.ticketly.mseventseatingprojection.dto.projection.EventProjectionDTO;
import com.ticketly.mseventseatingprojection.dto.projection.SeatingMapProjectionDTO;
import com.ticketly.mseventseatingprojection.dto.projection.SessionProjectionDTO;
import com.ticketly.mseventseatingprojection.dto.projection.TierInfo;
import com.ticketly.mseventseatingprojection.model.EventDocument;
import org.springframework.stereotype.Component;


import java.util.stream.Collectors;
import java.util.Map;

@Component
public class EventDocumentMapper {

    public EventDocument toEventDocument(EventProjectionDTO dto) {
        if (dto == null) return null;

        return EventDocument.builder()
                .id(dto.getId().toString())
                .title(dto.getTitle())
                .status(dto.getStatus().name())
                .description(dto.getDescription())
                .overview(dto.getOverview())
                .coverPhotos(dto.getCoverPhotos())
                .organization(toOrganizationInfo(dto.getOrganization()))
                .category(toCategoryInfo(dto.getCategory()))
                .tiers(dto.getTiers().stream().map(this::toTierInfo).collect(Collectors.toList()))
                .sessions(dto.getSessions().stream().map(this::toSessionInfo).collect(Collectors.toList()))
                .build();
    }

    public EventDocument.SessionInfo toSessionInfo(SessionProjectionDTO dto) {
        if (dto == null) return null;
        return EventDocument.SessionInfo.builder()
                .id(dto.getId().toString())
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .status(dto.getStatus())
                .sessionType(dto.getSessionType().name())
                .venueDetails(toVenueDetailsInfo(dto.getVenueDetails()))
                .build();
    }

    /**
     * Performs a deep mapping from the SeatingMapDTO (from Kafka) to the
     * corresponding EventDocument.SessionSeatingMapInfo model.
     * This method is now responsible for the "read-side join" logic, enriching
     * the final model with full tier details.
     *
     * @param dto         The SeatingMapProjectionDTO from the Kafka event.
     * @param tierInfoMap A lookup map of TierInfo objects from the EventDocument.
     * @return The corresponding, fully denormalized EventDocument.SessionSeatingMapInfo object.
     */
    public EventDocument.SessionSeatingMapInfo toSeatingMapInfo(SessionSeatingMapDTO dto, Map<String, EventDocument.TierInfo> tierInfoMap) {
        if (dto == null) return null;
        return EventDocument.SessionSeatingMapInfo.builder()
                .name(dto.getName())
                .layout(toLayoutInfo(dto.getLayout(), tierInfoMap))
                .build();
    }

    // --- Private helper methods for deep mapping ---

    private EventDocument.LayoutInfo toLayoutInfo(SessionSeatingMapDTO.Layout dto, Map<String, EventDocument.TierInfo> tierInfoMap) {
        if (dto == null) return null;
        return EventDocument.LayoutInfo.builder()
                .blocks(dto.getBlocks().stream().map(block -> toBlockInfo(block, tierInfoMap)).collect(Collectors.toList()))
                .build();
    }

    private EventDocument.BlockInfo toBlockInfo(SessionSeatingMapDTO.Block dto, Map<String, EventDocument.TierInfo> tierInfoMap) {
        if (dto == null) return null;
        return EventDocument.BlockInfo.builder()
                .id(dto.getId())
                .name(dto.getName())
                .type(dto.getType())
                .position(toPositionInfo(dto.getPosition()))
                .rows(dto.getRows() != null ? dto.getRows().stream().map(row -> toRowInfo(row, tierInfoMap)).collect(Collectors.toList()) : null)
                .seats(dto.getSeats() != null ? dto.getSeats().stream().map(seat -> toSeatInfo(seat, tierInfoMap)).collect(Collectors.toList()) : null)
                .capacity(dto.getCapacity())
                .width(dto.getWidth())
                .height(dto.getHeight())
                .build();
    }

    private EventDocument.RowInfo toRowInfo(SessionSeatingMapDTO.Row dto, Map<String, EventDocument.TierInfo> tierInfoMap) {
        if (dto == null) return null;
        return EventDocument.RowInfo.builder()
                .id(dto.getId())
                .label(dto.getLabel())
                .seats(dto.getSeats().stream().map(seat -> toSeatInfo(seat, tierInfoMap)).collect(Collectors.toList()))
                .build();
    }

    private EventDocument.SeatInfo toSeatInfo(SessionSeatingMapDTO.Seat dto, Map<String, EventDocument.TierInfo> tierInfoMap) {
        if (dto == null) return null;

        // We use the tierId from the DTO to look up the full TierInfo object.
        EventDocument.TierInfo embeddedTier = dto.getTierId() != null
                ? tierInfoMap.get(dto.getTierId())
                : null;

        return EventDocument.SeatInfo.builder()
                .id(dto.getId())
                .label(dto.getLabel())
                .status(dto.getStatus())
                .tier(embeddedTier) // Embed the full tier info
                .build();
    }

    private EventDocument.PositionInfo toPositionInfo(SessionSeatingMapDTO.Position dto) {
        if (dto == null) return null;
        return EventDocument.PositionInfo.builder()
                .x(dto.getX())
                .y(dto.getY())
                .build();
    }

    private EventDocument.OrganizationInfo toOrganizationInfo(EventProjectionDTO.OrganizationInfo dto) {
        if (dto == null) return null;
        return EventDocument.OrganizationInfo.builder()
                .id(dto.getId().toString())
                .name(dto.getName())
                .logoUrl(dto.getLogoUrl())
                .build();
    }

    private EventDocument.CategoryInfo toCategoryInfo(EventProjectionDTO.CategoryInfo dto) {
        if (dto == null) return null;
        return EventDocument.CategoryInfo.builder()
                .id(dto.getId().toString())
                .name(dto.getName())
                .parentName(dto.getParentName())
                .build();
    }

    private EventDocument.TierInfo toTierInfo(TierInfo dto) {
        if (dto == null) return null;
        return EventDocument.TierInfo.builder()
                .id(dto.getId().toString())
                .name(dto.getName())
                .price(dto.getPrice())
                .color(dto.getColor())
                .build();
    }

    private EventDocument.VenueDetailsInfo toVenueDetailsInfo(SessionProjectionDTO.VenueDetailsInfo dto) {
        if (dto == null) return null;
        return EventDocument.VenueDetailsInfo.builder()
                .name(dto.getName())
                .address(dto.getAddress())
                .onlineLink(dto.getOnlineLink())
                .location(toGeoJsonPoint(dto.getLocation()))
                .build();
    }

    private EventDocument.GeoJsonPoint toGeoJsonPoint(SessionProjectionDTO.GeoJsonPoint dto) {
        if (dto == null) return null;
        return EventDocument.GeoJsonPoint.builder()
                .coordinates(dto.getCoordinates())
                .build();
    }
}