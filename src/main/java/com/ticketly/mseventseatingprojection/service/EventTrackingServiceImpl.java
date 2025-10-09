package com.ticketly.mseventseatingprojection.service;

import com.ticketly.mseventseatingprojection.dto.analytics.EventViewsStatsDTO;
import com.ticketly.mseventseatingprojection.model.EventTrackingDocument;
import com.ticketly.mseventseatingprojection.repository.EventViewsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventTrackingServiceImpl implements EventTrackingService {

    private final EventViewsRepository eventViewsRepository;

    @Override
    public Mono<EventTrackingDocument> incrementViewCount(String eventId, String deviceType) {
        log.debug("Incrementing view count for event: {}, device type: {}", eventId, deviceType);
        LocalDate today = LocalDate.now();

        // Validate device type
        if (!isValidDeviceType(deviceType)) {
            return Mono.error(new IllegalArgumentException("Invalid device type. Must be 'mobile', 'desktop', 'tablet', or 'other'"));
        }

        return eventViewsRepository.findByEventIdAndTrackingBucketDate(eventId, today)
                .flatMap(document -> {
                    incrementDeviceViewCount(document.getTrackingBucket(), deviceType);
                    return eventViewsRepository.save(document);
                })
                .switchIfEmpty(
                        // Create new document if not exists
                        Mono.defer(() -> {
                            EventTrackingDocument.TrackingBucket bucket = EventTrackingDocument.TrackingBucket.builder()
                                    .date(today)
                                    .mobileViews(deviceType.equals("mobile") ? 1L : 0L)
                                    .desktopViews(deviceType.equals("desktop") ? 1L : 0L)
                                    .tabletViews(deviceType.equals("tablet") ? 1L : 0L)
                                    .otherViews(deviceType.equals("other") ? 1L : 0L)
                                    .orderCount(0L)
                                    .build();

                            EventTrackingDocument document = EventTrackingDocument.builder()
                                    .eventId(eventId)
                                    .trackingBucket(bucket)
                                    .build();

                            return eventViewsRepository.save(document);
                        })
                )
                .doOnSuccess(document -> log.info("Updated {} view count for event: {}, date: {}",
                        deviceType, eventId, today))
                .doOnError(error -> log.error("Error updating view count for event: {}", eventId, error));
    }

    /**
     * Validate if device type is one of the accepted values
     *
     * @param deviceType Device type string
     * @return True if valid
     */
    private boolean isValidDeviceType(String deviceType) {
        return deviceType != null &&
                (deviceType.equals("mobile") ||
                        deviceType.equals("desktop") ||
                        deviceType.equals("tablet") ||
                        deviceType.equals("other"));
    }

    /**
     * Increment the appropriate device view count field
     *
     * @param bucket Tracking bucket
     * @param deviceType Device type
     */
    private void incrementDeviceViewCount(EventTrackingDocument.TrackingBucket bucket, String deviceType) {
        switch (deviceType) {
            case "mobile":
                bucket.setMobileViews(bucket.getMobileViews() + 1);
                break;
            case "desktop":
                bucket.setDesktopViews(bucket.getDesktopViews() + 1);
                break;
            case "tablet":
                bucket.setTabletViews(bucket.getTabletViews() + 1);
                break;
            case "other":
                bucket.setOtherViews(bucket.getOtherViews() + 1);
                break;
            default:
                // Already validated, shouldn't get here
                break;
        }
    }

@Override
public Mono<EventTrackingDocument> incrementOrderCount(String eventId) {
    log.debug("Incrementing order count for event: {}", eventId);
    LocalDate today = LocalDate.now();

    return eventViewsRepository.findByEventIdAndTrackingBucketDate(eventId, today)
            .flatMap(document -> {
                // Increment existing document
                document.getTrackingBucket().setOrderCount(document.getTrackingBucket().getOrderCount() + 1);
                return eventViewsRepository.save(document);
            })
            .switchIfEmpty(
                    // Create new document if not exists
                    Mono.defer(() -> {
                        EventTrackingDocument.TrackingBucket bucket = EventTrackingDocument.TrackingBucket.builder()
                                .date(today)
                                .mobileViews(0L)
                                .desktopViews(0L)
                                .tabletViews(0L)
                                .otherViews(0L)
                                .orderCount(1L)
                                .build();

                        EventTrackingDocument document = EventTrackingDocument.builder()
                                .eventId(eventId)
                                .trackingBucket(bucket)
                                .build();

                        return eventViewsRepository.save(document);
                    })
            )
            .doOnSuccess(document -> log.info("Updated order count for event: {}, date: {}, new count: {}",
                    eventId, today, document.getTrackingBucket().getOrderCount()))
            .doOnError(error -> log.error("Error updating order count for event: {}", eventId, error));
}

@Override
public Mono<EventViewsStatsDTO> getEventViewsStats(String eventId, LocalDate fromDate, LocalDate toDate) {
    log.debug("Getting event views stats for event: {}, date range: {} to {}", eventId, fromDate, toDate);

    return eventViewsRepository.findByEventId(eventId)
            .filter(doc -> {
                LocalDate docDate = doc.getTrackingBucket().getDate();
                return !docDate.isBefore(fromDate) && !docDate.isAfter(toDate);
            })
            .collectList()
            .map(documents -> {
                long totalMobileViews = documents.stream()
                        .mapToLong(doc -> doc.getTrackingBucket().getMobileViews())
                        .sum();

                long totalDesktopViews = documents.stream()
                        .mapToLong(doc -> doc.getTrackingBucket().getDesktopViews())
                        .sum();

                long totalTabletViews = documents.stream()
                        .mapToLong(doc -> doc.getTrackingBucket().getTabletViews())
                        .sum();
                        
                long totalOtherViews = documents.stream()
                        .mapToLong(doc -> doc.getTrackingBucket().getOtherViews())
                        .sum();

                long totalOrders = documents.stream()
                        .mapToLong(doc -> doc.getTrackingBucket().getOrderCount())
                        .sum();

                List<EventViewsStatsDTO.DailyStats> dailyStats = documents.stream()
                        .map(doc -> EventViewsStatsDTO.DailyStats.builder()
                                .date(doc.getTrackingBucket().getDate())
                                .mobileViews(doc.getTrackingBucket().getMobileViews())
                                .desktopViews(doc.getTrackingBucket().getDesktopViews())
                                .tabletViews(doc.getTrackingBucket().getTabletViews())
                                .otherViews(doc.getTrackingBucket().getOtherViews())
                                .orderCount(doc.getTrackingBucket().getOrderCount())
                                .build())
                        .collect(Collectors.toList());

                return EventViewsStatsDTO.builder()
                        .eventId(eventId)
                        .totalMobileViews(totalMobileViews)
                        .totalDesktopViews(totalDesktopViews)
                        .totalTabletViews(totalTabletViews)
                        .totalOtherViews(totalOtherViews)
                        .totalOrders(totalOrders)
                        .fromDate(fromDate)
                        .toDate(toDate)
                        .dailyStats(dailyStats)
                        .build();
            })
            .doOnSuccess(stats -> log.info("Retrieved stats for event: {}, mobile: {}, desktop: {}, tablet: {}, other: {}, orders: {}",
                    eventId, stats.getTotalMobileViews(), stats.getTotalDesktopViews(),
                    stats.getTotalTabletViews(), stats.getTotalOtherViews(), stats.getTotalOrders()))
            .doOnError(error -> log.error("Error retrieving stats for event: {}", eventId, error));
}

@Override
public Flux<EventTrackingDocument> getAllEventAnalytics(String eventId) {
    log.debug("Getting all analytics for event: {}", eventId);
    return eventViewsRepository.findByEventId(eventId)
            .doOnComplete(() -> log.info("Retrieved all analytics for event: {}", eventId))
            .doOnError(error -> log.error("Error retrieving analytics for event: {}", eventId, error));
}

@Override
public Mono<Void> deleteEventAnalytics(String eventId) {
    log.debug("Deleting all analytics for event: {}", eventId);
    return eventViewsRepository.deleteByEventId(eventId)
            .doOnSuccess(v -> log.info("Deleted all analytics for event: {}", eventId))
            .doOnError(error -> log.error("Error deleting analytics for event: {}", eventId, error));
}
}