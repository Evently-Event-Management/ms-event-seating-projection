package com.ticketly.mseventseatingprojection.service.impl;

import com.ticketly.mseventseatingprojection.dto.analytics.EventAnalyticsDTO;
import com.ticketly.mseventseatingprojection.dto.read.EventThumbnailDTO;
import com.ticketly.mseventseatingprojection.model.EventTrendingDocument;
import com.ticketly.mseventseatingprojection.repository.EventRepository;
import com.ticketly.mseventseatingprojection.repository.EventTrendingRepository;
import com.ticketly.mseventseatingprojection.repository.TrendingRepositoryCustom;
import com.ticketly.mseventseatingprojection.service.EventAnalyticsService;
import com.ticketly.mseventseatingprojection.service.EventTrendingService;
import com.ticketly.mseventseatingprojection.service.GoogleAnalyticsService;
import com.ticketly.mseventseatingprojection.service.mapper.EventQueryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventTrendingServiceImpl implements EventTrendingService {

    private final EventTrendingRepository eventTrendingRepository;
    private final EventRepository eventRepository;
    private final EventAnalyticsService eventAnalyticsService;
    private final GoogleAnalyticsService googleAnalyticsService;
    private final TrendingRepositoryCustom trendingRepositoryCustom;
    private final EventQueryMapper eventMapper;

    @Override
    public Mono<EventTrendingDocument> getEventTrendingScore(String eventId) {
        return eventTrendingRepository.findByEventId(eventId)
                .switchIfEmpty(Mono.defer(() -> 
                    // If not found, calculate it on-demand
                    calculateAndUpdateTrendingScore(eventId)
                ));
    }

    @Override
    public Mono<EventTrendingDocument> calculateAndUpdateTrendingScore(String eventId) {
        log.info("Calculating trending score for eventId={}", eventId);
        
        Mono<EventAnalyticsDTO> analyticsMono = eventAnalyticsService.getEventAnalytics(eventId)
            .onErrorResume(e -> {
                log.error("Error fetching event analytics for eventId={}: {}", eventId, e.getMessage());
                return Mono.empty();
            });
            
        Mono<Integer> viewsMono = googleAnalyticsService.getEventTotalViews(eventId)
            .onErrorResume(e -> {
                log.error("Error fetching event views for eventId={}: {}", eventId, e.getMessage());
                return Mono.just(0);
            });
            
        Mono<EventTrendingDocument> trendingDocMono = eventTrendingRepository.findByEventId(eventId)
            .defaultIfEmpty(EventTrendingDocument.builder()
                .eventId(eventId)
                .trendingScore(0.0)
                .viewCount(0)
                .purchaseCount(0)
                .reservationCount(0)
                .build());
        
        return Mono.zip(analyticsMono, viewsMono, trendingDocMono)
            .flatMap(tuple -> {
                EventAnalyticsDTO analytics = tuple.getT1();
                Integer viewCount = tuple.getT2();
                EventTrendingDocument trendingDoc = tuple.getT3();
                
                // Set the view count from Google Analytics
                trendingDoc.setViewCount(viewCount);
                
                // If we have analytics data, use it
                if (analytics != null) {
                    trendingDoc.setPurchaseCount(analytics.getTotalTicketsSold());

                    // Calculate trending score based on views, purchases, and reservations
                    double score = calculateTrendingScore(viewCount, analytics.getTotalTicketsSold(), analytics.getOverallSellOutPercentage());
                    trendingDoc.setTrendingScore(score);

                    log.info("Calculated trending score for eventId={}: score={}, views={}, purchases={}, reservations={}",
                            eventId, score, viewCount, analytics.getTotalTicketsSold(), analytics.getOverallSellOutPercentage());
                }

                trendingDoc.setLastCalculated(Instant.now());
                trendingDoc.setLastUpdated(Instant.now());
                
                return eventTrendingRepository.save(trendingDoc);
            })
            .onErrorResume(e -> {
                log.error("Error calculating trending score for eventId={}: {}", eventId, e.getMessage(), e);
                return Mono.empty();
            });
    }

    @Override
    public Flux<EventTrendingDocument> calculateAndUpdateAllTrendingScores() {
        log.info("Calculating trending scores for all events");
        
        return eventRepository.findAll()
                .flatMap(event -> calculateAndUpdateTrendingScore(event.getId())
                        .onErrorResume(e -> {
                            log.error("Error calculating trending score for eventId={}: {}", 
                                    event.getId(), e.getMessage());
                            return Mono.empty();
                        }));
    }

    @Override
    public Flux<EventTrendingDocument> getTopTrendingEvents(int limit) {
        return eventTrendingRepository.findAll()
                .sort((a, b) -> Double.compare(b.getTrendingScore(), a.getTrendingScore()))
                .take(limit);
    }
    
    @Override
    public Flux<EventThumbnailDTO> getTopTrendingEventThumbnails(int limit) {
        log.info("Getting top {} trending event thumbnails", limit);
        return trendingRepositoryCustom.findTopTrendingEvents(limit)
                .doOnNext(event -> log.info("Processing event from repository: id={}, title={}", event.getId(), event.getTitle()))
                .map(eventMapper::mapToThumbnailDTO)
                .doOnNext(dto -> log.info("Mapped trending event to thumbnail: id={}, title={}", dto.getId(), dto.getTitle()))
                .doOnComplete(() -> log.info("Completed getting trending event thumbnails"))
                .doOnError(e -> log.error("Error getting trending event thumbnails: {}", e.getMessage()));
    }
    
    /**
     * Calculate trending score using a weighted formula
     * Formula:
     * score = (viewWeight * views + purchaseWeight * purchases + selloutPercentageWeight * selloutPercentage) * timeDecayFactor
     * 
     * @param views Number of views
     * @param purchases Number of purchases
     * @param selloutPercentage Sellout percentage of the event
     * @return Calculated trending score
     */
    private double calculateTrendingScore(int views, int purchases, double selloutPercentage) {
        // Weights for different factors (can be adjusted)
        double viewWeight = 1.0;
        double purchaseWeight = 10.0;  // Purchases are more important than views
        double selloutPercentageWeight = 5.0; // Sellout percentage impact

        // Calculate base score

        return (viewWeight * views +
                        purchaseWeight * purchases +
                        selloutPercentageWeight * selloutPercentage);
    }
}