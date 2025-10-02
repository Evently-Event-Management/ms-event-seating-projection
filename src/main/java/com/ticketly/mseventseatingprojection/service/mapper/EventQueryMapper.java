package com.ticketly.mseventseatingprojection.service.mapper;

import com.ticketly.mseventseatingprojection.dto.SessionInfoDTO;
import com.ticketly.mseventseatingprojection.dto.read.DiscountDetailsDTO;
import com.ticketly.mseventseatingprojection.dto.read.DiscountThumbnailDTO;
import com.ticketly.mseventseatingprojection.dto.read.EventBasicInfoDTO;
import com.ticketly.mseventseatingprojection.dto.read.EventThumbnailDTO;
import com.ticketly.mseventseatingprojection.model.EventDocument;
import dto.projection.discount.BogoDiscountParamsDTO;
import dto.projection.discount.DiscountParametersDTO;
import dto.projection.discount.FlatOffDiscountParamsDTO;
import dto.projection.discount.PercentageDiscountParamsDTO;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class EventQueryMapper extends BaseMapper {

    public EventThumbnailDTO mapToThumbnailDTO(EventDocument event) {
        // Find the earliest upcoming session
        EventDocument.SessionInfo earliestSession = event.getSessions().stream()
                .filter(s -> s.getStartTime().isAfter(Instant.now()))
                .min(Comparator.comparing(EventDocument.SessionInfo::getStartTime))
                .orElse(event.getSessions().stream().findFirst().orElse(null));

        // ✅ CORRECTED LOGIC: Filter for public, active discounts that are applicable to the earliest session
        List<DiscountThumbnailDTO> availableDiscounts = (event.getDiscounts() == null) ? Collections.emptyList()
                : event.getDiscounts().stream()
                .filter(this::isDiscountCurrentlyValid)
                .map(this::mapToDiscountThumbnailDTO)
                .collect(Collectors.toList());

        // Find the lowest priced tier
        BigDecimal startingPrice = (event.getTiers() == null) ? BigDecimal.ZERO
                : event.getTiers().stream()
                .map(EventDocument.TierInfo::getPrice)
                .min(Comparator.naturalOrder())
                .orElse(BigDecimal.ZERO);

        EventThumbnailDTO.EarliestSessionInfo sessionInfo = null;
        if (earliestSession != null) {
            sessionInfo = EventThumbnailDTO.EarliestSessionInfo.builder()
                    .startTime(earliestSession.getStartTime())
                    .venueName(earliestSession.getVenueDetails() != null ? earliestSession.getVenueDetails().getName() : "Online")
                    .city(extractCity(earliestSession.getVenueDetails()))
                    .build();
        }

        return EventThumbnailDTO.builder()
                .id(event.getId())
                .title(event.getTitle())
                .coverPhotoUrl(event.getCoverPhotos() != null && !event.getCoverPhotos().isEmpty()
                        ? event.getCoverPhotos().getFirst()
                        : null)
                .organizationName(event.getOrganization().getName())
                .categoryName(event.getCategory().getName())
                .discounts(availableDiscounts)
                .earliestSession(sessionInfo)
                .startingPrice(startingPrice)
                .build();
    }

    /**
     * A helper method to determine if a discount is active, public, and valid for a specific session.
     */
    public boolean isDiscountCurrentlyValid(EventDocument.DiscountInfo discount) {
        Instant now = Instant.now();
        return discount != null && discount.isPublic() && discount.isActive() &&
                (discount.getActiveFrom() == null || !discount.getActiveFrom().isAfter(now)) &&
                (discount.getExpiresAt() == null || !discount.getExpiresAt().isBefore(now));
    }

    public DiscountThumbnailDTO mapToDiscountThumbnailDTO(EventDocument.DiscountInfo discountInfo) {
        if (discountInfo == null) {
            return null;
        }
        return DiscountThumbnailDTO.builder()
                .parameters(mapToDiscountParameters(discountInfo.getParameters()))
                .maxUsage(discountInfo.getMaxUsage())
                .currentUsage(discountInfo.getCurrentUsage())
                .expiresAt(discountInfo.getExpiresAt())
                .build();
    }

    public DiscountDetailsDTO mapToDiscountDetailsDTO(EventDocument.DiscountInfo discountInfo) {
        if (discountInfo == null) {
            return null;
        }
        return DiscountDetailsDTO.builder()
                .id(discountInfo.getId())
                .code(discountInfo.getCode())
                .parameters(mapToDiscountParameters(discountInfo.getParameters()))
                .isActive(discountInfo.isActive())
                .isPublic(discountInfo.isPublic())
                .activeFrom(discountInfo.getActiveFrom())
                .expiresAt(discountInfo.getExpiresAt())
                .maxUsage(discountInfo.getMaxUsage())
                .applicableTiers(discountInfo.getApplicableTiers() != null
                        ? discountInfo.getApplicableTiers().stream()
                        .map(tier -> DiscountDetailsDTO.TierInfo.builder()
                                .id(tier.getId())
                                .name(tier.getName())
                                .price(tier.getPrice())
                                .color(tier.getColor())
                                .build())
                        .collect(Collectors.toList())
                        : null)
                .currentUsage(discountInfo.getCurrentUsage())
                .build();
    }

    private DiscountParametersDTO mapToDiscountParameters(EventDocument.DiscountParametersInfo paramsInfo) {
        if (paramsInfo == null || paramsInfo.getType() == null) {
            return null;
        }
        return switch (paramsInfo.getType()) {
            case PERCENTAGE -> new PercentageDiscountParamsDTO(paramsInfo.getType(), paramsInfo.getPercentage(), paramsInfo.getMinSpend(), paramsInfo.getMaxDiscount());
            case FLAT_OFF ->
                    new FlatOffDiscountParamsDTO(paramsInfo.getType(), paramsInfo.getAmount(), paramsInfo.getCurrency(), paramsInfo.getMinSpend());
            case BUY_N_GET_N_FREE ->
                    new BogoDiscountParamsDTO(paramsInfo.getType(), paramsInfo.getBuyQuantity(), paramsInfo.getGetQuantity());
        };
    }

    public SessionInfoDTO mapToSessionInfoDTO(EventDocument.SessionInfo session) {
        SessionInfoDTO.VenueDetailsInfo venueDetailsDTO = null;
        if (session.getVenueDetails() != null) {
            venueDetailsDTO = SessionInfoDTO.VenueDetailsInfo.builder()
                    .name(session.getVenueDetails().getName())
                    .address(session.getVenueDetails().getAddress())
                    .onlineLink(session.getVenueDetails().getOnlineLink())
                    .location(session.getVenueDetails().getLocation())
                    .build();
        }

        return SessionInfoDTO.builder()
                .id(session.getId())
                .startTime(session.getStartTime())
                .endTime(session.getEndTime())
                .status(session.getStatus())
                .sessionType(session.getSessionType())
                .venueDetails(venueDetailsDTO)
                .salesStartTime(session.getSalesStartTime())
                .build();
    }

    private String extractCity(EventDocument.VenueDetailsInfo venue) {
        if (venue == null || venue.getAddress() == null || venue.getAddress().isBlank()) {
            return null;
        }
        String[] parts = venue.getAddress().split(",");
        return parts.length > 1 ? parts[1].trim() : parts[0].trim();
    }
    
    /**
     * Maps an EventDocument to EventBasicInfoDTO
     * 
     * @param event The event document
     * @return The mapped EventBasicInfoDTO
     */
    public EventBasicInfoDTO mapToBasicInfoDTO(EventDocument event) {
        return EventBasicInfoDTO.builder()
                .id(event.getId())
                .title(event.getTitle())
                .description(event.getDescription())
                .overview(event.getOverview())
                .coverPhotos(event.getCoverPhotos())
                .organization(event.getOrganization() != null ? EventBasicInfoDTO.OrganizationInfo.builder()
                        .id(event.getOrganization().getId())
                        .name(event.getOrganization().getName())
                        .logoUrl(event.getOrganization().getLogoUrl())
                        .build() : null)
                .category(event.getCategory() != null ? EventBasicInfoDTO.CategoryInfo.builder()
                        .id(event.getCategory().getId())
                        .name(event.getCategory().getName())
                        .parentName(event.getCategory().getParentName())
                        .build() : null)
                .tiers(event.getTiers() != null ? event.getTiers().stream()
                        .map(tier -> EventBasicInfoDTO.TierInfo.builder()
                                .id(tier.getId())
                                .name(tier.getName())
                                .price(tier.getPrice())
                                .color(tier.getColor())
                                .build())
                        .collect(Collectors.toList()) : Collections.emptyList())
                .availableDiscounts(event.getDiscounts() != null ? event.getDiscounts().stream()
                        .filter(this::isDiscountCurrentlyValid)
                        .map(this::mapToDiscountThumbnailDTO)
                        .collect(Collectors.toList()) : Collections.emptyList())
                .build();
    }
}