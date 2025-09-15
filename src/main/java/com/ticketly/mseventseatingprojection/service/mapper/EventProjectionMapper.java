package com.ticketly.mseventseatingprojection.service.mapper;

import com.ticketly.mseventseatingprojection.model.EventDocument;
import com.ticketly.mseventseatingprojection.model.ReadModelSeatStatus;
import com.ticketly.mseventseatingprojection.service.S3UrlGenerator;
import dto.projection.EventProjectionDTO;
import dto.projection.SeatingMapProjectionDTO;
import dto.projection.SessionProjectionDTO;
import dto.projection.TierInfo;
import lombok.AllArgsConstructor;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@AllArgsConstructor
public class EventProjectionMapper {

    private final S3UrlGenerator s3UrlGenerator;

    /**
     * Maps an EventProjectionDTO to an EventDocument.
     *
     * @param dto the EventProjectionDTO to map
     * @return the mapped EventDocument
     */
    public EventDocument fromProjection(EventProjectionDTO dto) {
        if (dto == null) return null;

        List<String> publicCoverPhotoUrls = dto.getCoverPhotos() != null
                ? dto.getCoverPhotos().stream()
                .map(s3UrlGenerator::generatePublicUrl)
                .toList()
                : null;

        return EventDocument.builder()
                .id(dto.getId().toString())
                .title(dto.getTitle())
                .status(dto.getStatus())
                .description(dto.getDescription())
                .overview(dto.getOverview())
                .coverPhotos(publicCoverPhotoUrls)
                .organization(fromOrganization(dto.getOrganization()))
                .category(fromCategory(dto.getCategory()))
                .tiers(mapList(dto.getTiers(), this::fromTier))
                .sessions(mapList(dto.getSessions(), this::fromSession))
                .build();
    }

    /**
     * Maps a SessionProjectionDTO to an EventDocument.SessionInfo.
     *
     * @param dto the SessionProjectionDTO to map
     * @return the mapped SessionInfo
     */
    public EventDocument.SessionInfo fromSession(SessionProjectionDTO dto) {
        if (dto == null) return null;
        return EventDocument.SessionInfo.builder()
                .id(dto.getId().toString())
                .startTime(dto.getStartTime().toInstant())
                .endTime(dto.getEndTime().toInstant())
                .status(dto.getSessionStatus())
                .salesStartTime(dto.getSalesStartTime().toInstant())
                .sessionType(dto.getSessionType())
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

        ReadModelSeatStatus status = null;
        if (dto.getStatus() != null) {
            try {
                status = ReadModelSeatStatus.valueOf(dto.getStatus().toString().toUpperCase());
            } catch (IllegalArgumentException e) {
                // Handle cases where the string might be invalid, default to AVAILABLE
                status = ReadModelSeatStatus.AVAILABLE;
            }
        }

        return EventDocument.SeatInfo.builder()
                .id(dto.getId())
                .label(dto.getLabel())
                .status(status)
                .tier(fromTier(dto.getTier()))
                .build();
    }

    private EventDocument.OrganizationInfo fromOrganization(EventProjectionDTO.OrganizationInfo dto) {
        if (dto == null) return null;
        return EventDocument.OrganizationInfo.builder()
                .id(dto.getId().toString())
                .name(dto.getName())
                .logoUrl(s3UrlGenerator.generatePublicUrl(dto.getLogoUrl()))
                .userId(dto.getUserId())
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
        GeoJsonPoint location = null;

        if (dto.getLatitude() != null && dto.getLongitude() != null) {
            location = new GeoJsonPoint(dto.getLongitude(), dto.getLatitude());
        }

        return EventDocument.VenueDetailsInfo.builder()
                .name(dto.getName())
                .address(dto.getAddress())
                .onlineLink(dto.getOnlineLink())
                .location(location)
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
