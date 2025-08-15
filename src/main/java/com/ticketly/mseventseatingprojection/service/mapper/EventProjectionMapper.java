package com.ticketly.mseventseatingprojection.service.mapper;

import com.ticketly.mseventseatingprojection.dto.projection.EventProjectionDTO;
import com.ticketly.mseventseatingprojection.dto.projection.SeatingMapProjectionDTO;
import com.ticketly.mseventseatingprojection.dto.projection.SessionProjectionDTO;
import com.ticketly.mseventseatingprojection.dto.projection.TierInfo;
import com.ticketly.mseventseatingprojection.model.EventDocument;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class EventProjectionMapper {

    public EventDocument fromProjection(EventProjectionDTO dto) {
        if (dto == null) return null;
        return EventDocument.builder()
                .id(dto.getId().toString())
                .title(dto.getTitle())
                .status(dto.getStatus().name())
                .description(dto.getDescription())
                .overview(dto.getOverview())
                .coverPhotos(dto.getCoverPhotos())
                .organization(fromOrganization(dto.getOrganization()))
                .category(fromCategory(dto.getCategory()))
                .tiers(mapList(dto.getTiers(), this::fromTier))
                .sessions(mapList(dto.getSessions(), this::fromSession))
                .build();
    }

    public EventDocument.SessionInfo fromSession(SessionProjectionDTO dto) {
        if (dto == null) return null;
        return EventDocument.SessionInfo.builder()
                .id(dto.getId().toString())
                .startTime(dto.getStartTime().toInstant())
                .endTime(dto.getEndTime().toInstant())
                .status(dto.getStatus())
                .sessionType(dto.getSessionType().name())
                .layoutData(fromSeatingMap(dto.getLayoutData()))
                .venueDetails(fromVenue(dto.getVenueDetails()))
                .build();
    }

    private EventDocument.SessionSeatingMapInfo fromSeatingMap(SeatingMapProjectionDTO dto) {
        if (dto == null) return null;
        return EventDocument.SessionSeatingMapInfo.builder()
                .name(dto.getName())
                .layout(fromLayout(dto.getLayout()))
                .build();
    }

    private EventDocument.LayoutInfo fromLayout(SeatingMapProjectionDTO.LayoutInfo dto) {
        if (dto == null) return null;
        return EventDocument.LayoutInfo.builder()
                .blocks(mapList(dto.getBlocks(), this::fromBlock))
                .build();
    }

    private EventDocument.BlockInfo fromBlock(SeatingMapProjectionDTO.BlockInfo dto) {
        if (dto == null) return null;
        return EventDocument.BlockInfo.builder()
                .id(dto.getId())
                .name(dto.getName())
                .type(dto.getType())
                .position(fromPosition(dto.getPosition()))
                .rows(mapList(dto.getRows(), this::fromRow))
                .seats(mapList(dto.getSeats(), this::fromSeat))
                .capacity(dto.getCapacity())
                .width(dto.getWidth())
                .height(dto.getHeight())
                .build();
    }

    private EventDocument.RowInfo fromRow(SeatingMapProjectionDTO.RowInfo dto) {
        if (dto == null) return null;
        return EventDocument.RowInfo.builder()
                .id(dto.getId())
                .label(dto.getLabel())
                .seats(mapList(dto.getSeats(), this::fromSeat))
                .build();
    }

    private EventDocument.SeatInfo fromSeat(SeatingMapProjectionDTO.SeatInfo dto) {
        if (dto == null) return null;
        return EventDocument.SeatInfo.builder()
                .id(dto.getId())
                .label(dto.getLabel())
                .status(dto.getStatus())
                .tier(fromTier(dto.getTier()))
                .build();
    }

    private EventDocument.OrganizationInfo fromOrganization(EventProjectionDTO.OrganizationInfo dto) {
        if (dto == null) return null;
        return EventDocument.OrganizationInfo.builder()
                .id(dto.getId().toString())
                .name(dto.getName())
                .logoUrl(dto.getLogoUrl())
                .build();
    }

    private EventDocument.CategoryInfo fromCategory(EventProjectionDTO.CategoryInfo dto) {
        if (dto == null) return null;
        return EventDocument.CategoryInfo.builder()
                .id(dto.getId().toString())
                .name(dto.getName())
                .parentName(dto.getParentName())
                .build();
    }

    private EventDocument.TierInfo fromTier(TierInfo dto) {
        if (dto == null) return null;
        return EventDocument.TierInfo.builder()
                .id(dto.getId().toString())
                .name(dto.getName())
                .price(dto.getPrice())
                .color(dto.getColor())
                .build();
    }

    private EventDocument.VenueDetailsInfo fromVenue(SessionProjectionDTO.VenueDetailsInfo dto) {
        if (dto == null) return null;
        return EventDocument.VenueDetailsInfo.builder()
                .name(dto.getName())
                .address(dto.getAddress())
                .onlineLink(dto.getOnlineLink())
                .location(fromGeoJson(dto.getLocation()))
                .build();
    }

    private EventDocument.GeoJsonPoint fromGeoJson(SessionProjectionDTO.GeoJsonPoint dto) {
        if (dto == null) return null;
        return EventDocument.GeoJsonPoint.builder()
                .coordinates(dto.getCoordinates())
                .build();
    }

    private EventDocument.PositionInfo fromPosition(SeatingMapProjectionDTO.PositionInfo dto) {
        if (dto == null) return null;
        return EventDocument.PositionInfo.builder()
                .x(dto.getX())
                .y(dto.getY())
                .build();
    }

    private <T, R> java.util.List<R> mapList(java.util.List<T> list, java.util.function.Function<T, R> mapper) {
        return list == null ? null : list.stream().map(mapper).collect(Collectors.toList());
    }
}
