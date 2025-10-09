package com.ticketly.mseventseatingprojection.service;

import com.ticketly.mseventseatingprojection.model.EventTrackingDocument;
import com.ticketly.mseventseatingprojection.repository.EventTrackingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;
@Service
@RequiredArgsConstructor
@Slf4j
public class EventTrackingServiceImpl implements EventTrackingService {

    private final EventTrackingRepository eventTrackingRepository;

    @Override
    public Mono<EventTrackingDocument> incrementViewCount(String eventId, String deviceType) {
        log.debug("Incrementing view count for event: {}, device type: {}", eventId, deviceType);
        LocalDate today = LocalDate.now();

        if (!isValidDeviceType(deviceType)) {
            return Mono.error(new IllegalArgumentException("Invalid device type."));
        }

        // 1. Find the document by eventId only
        return eventTrackingRepository.findByEventId(eventId)
                .flatMap(document -> {
                    // 2. Find today's bucket within the existing document
                    EventTrackingDocument.TrackingBucket todayBucket = document.getTrackingBuckets().stream()
                            .filter(bucket -> bucket.getDate().equals(today))
                            .findFirst()
                            .orElseGet(() -> {
                                // 3. If bucket doesn't exist for today, create and add it
                                EventTrackingDocument.TrackingBucket newBucket = EventTrackingDocument.TrackingBucket.builder()
                                        .date(today)
                                        .mobileViews(0L).desktopViews(0L).tabletViews(0L).otherViews(0L)
                                        .orderCount(0L)
                                        .build();
                                document.getTrackingBuckets().add(newBucket);
                                return newBucket;
                            });

                    // 4. Increment the count and save the updated document
                    incrementDeviceViewCount(todayBucket, deviceType);
                    return eventTrackingRepository.save(document);
                })
                .switchIfEmpty(
                        // 5. If no document exists for the eventId, create a new one
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
                                    .trackingBuckets(new ArrayList<>(List.of(bucket))) // Initialize list with the new bucket
                                    .build();

                            return eventTrackingRepository.save(document);
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
     * @param bucket     Tracking bucket
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
    public Mono<Void> deleteEventAnalytics(String eventId) {
        log.debug("Deleting all analytics for event: {}", eventId);
        return eventTrackingRepository.deleteByEventId(eventId)
                .doOnSuccess(v -> log.info("Deleted all analytics for event: {}", eventId))
                .doOnError(error -> log.error("Error deleting analytics for event: {}", eventId, error));
    }
}