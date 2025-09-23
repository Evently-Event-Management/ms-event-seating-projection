package com.ticketly.mseventseatingprojection.service.mapper;

import com.ticketly.mseventseatingprojection.model.EventDocument;
import com.ticketly.mseventseatingprojection.service.S3UrlGenerator;
import dto.projection.*;
import dto.projection.discount.BogoDiscountParamsProjectionDTO;
import dto.projection.discount.DiscountParametersProjectionDTO;
import dto.projection.discount.FlatOffDiscountParamsProjectionDTO;
import dto.projection.discount.PercentageDiscountParamsProjectionDTO;
import lombok.AllArgsConstructor;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@AllArgsConstructor
public class EventProjectionMapper {

    private final S3UrlGenerator s3UrlGenerator;
    private final SeatingMapMapper seatingMapMapper;

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
                .discounts(mapList(dto.getDiscounts(), this::fromDiscount))
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
                .layoutData(seatingMapMapper.fromProjection(dto.getLayoutData()))
                .venueDetails(fromVenue(dto.getVenueDetails()))
                .build();
    }

    public EventDocument.DiscountInfo fromDiscount(DiscountProjectionDTO dto) {
        if (dto == null) return null;
        return EventDocument.DiscountInfo.builder()
                .id(dto.getId().toString())
                .code(dto.getCode())
                .parameters(fromDiscountParameters(dto.getParameters()))
                .maxUsage(dto.getMaxUsage())
                .currentUsage(dto.getCurrentUsage())
                .isActive(dto.isActive())
                .isPublic(dto.isPublic())
                .activeFrom(dto.getActiveFrom() != null ? dto.getActiveFrom().toInstant() : null)
                .expiresAt(dto.getExpiresAt() != null ? dto.getExpiresAt().toInstant() : null)
                .applicableTierIds(dto.getApplicableTierIds() != null
                        ? dto.getApplicableTierIds().stream().map(Object::toString).collect(Collectors.toList())
                        : null)
                .applicableSessionIds(dto.getApplicableSessionIds() != null
                        ? dto.getApplicableSessionIds().stream().map(Object::toString).collect(Collectors.toList())
                        : null)
                .build();
    }

    private EventDocument.DiscountParametersInfo fromDiscountParameters(DiscountParametersProjectionDTO dto) {
        return switch (dto) {
            case PercentageDiscountParamsProjectionDTO p ->
                    EventDocument.DiscountParametersInfo.builder()
                            .type(p.getType())
                            .percentage(p.getPercentage())
                            .build();
            case FlatOffDiscountParamsProjectionDTO f ->
                    EventDocument.DiscountParametersInfo.builder()
                            .type(f.getType())
                            .amount(f.getAmount())
                            .currency(f.getCurrency())
                            .build();
            case BogoDiscountParamsProjectionDTO b ->
                    EventDocument.DiscountParametersInfo.builder()
                            .type(b.getType())
                            .buyQuantity(b.getBuyQuantity())
                            .getQuantity(b.getGetQuantity())
                            .build();
            case null, default -> null;
        };

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
