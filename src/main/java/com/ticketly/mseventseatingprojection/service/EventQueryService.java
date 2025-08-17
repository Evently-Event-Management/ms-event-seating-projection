package com.ticketly.mseventseatingprojection.service;

import com.ticketly.mseventseatingprojection.dto.read.EventThumbnailDTO;
import com.ticketly.mseventseatingprojection.model.EventDocument;
import com.ticketly.mseventseatingprojection.repository.EventReadRepositoryCustomImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;

@Service
@RequiredArgsConstructor
public class EventQueryService {

    private final EventReadRepositoryCustomImpl eventReadRepository;

    public Mono<Page<EventThumbnailDTO>> searchEvents(
            String searchTerm, String categoryId, Double longitude, Double latitude,
            Integer radiusKm, Instant dateFrom, Instant dateTo,
            BigDecimal priceMin, BigDecimal priceMax, Pageable pageable
    ) {
        return eventReadRepository.searchEvents(
                searchTerm, categoryId, longitude, latitude, radiusKm,
                dateFrom, dateTo, priceMin, priceMax, pageable
        ).map(eventPage -> eventPage.map(this::mapToThumbnailDTO));
    }



    private EventThumbnailDTO mapToThumbnailDTO(EventDocument event) {
        // Find the earliest upcoming session
        EventDocument.SessionInfo earliestSession = event.getSessions().stream()
                .filter(s -> s.getStartTime().isAfter(Instant.now()))
                .min(Comparator.comparing(EventDocument.SessionInfo::getStartTime))
                .orElse(event.getSessions().stream().findFirst().orElse(null));

        // Find the lowest priced tier
        BigDecimal startingPrice = event.getTiers().stream()
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
                .earliestSession(sessionInfo)
                .startingPrice(startingPrice)
                .build();
    }

    private String extractCity(EventDocument.VenueDetailsInfo venue) {
        if (venue == null || venue.getAddress() == null || venue.getAddress().isBlank()) {
            return null;
        }
        // Simple city extraction logic, can be improved
        String[] parts = venue.getAddress().split(",");
        return parts.length > 1 ? parts[1].trim() : parts[0].trim();
    }
}
