package com.ticketly.mseventseatingprojection.service;

import com.ticketly.mseventseatingprojection.dto.analytics.*;
import com.ticketly.mseventseatingprojection.dto.analytics.raw.EventOverallStatsDTO;
import com.ticketly.mseventseatingprojection.dto.analytics.raw.SeatStatusCountDTO;
import com.ticketly.mseventseatingprojection.dto.analytics.raw.SessionStatusCountDTO;
import com.ticketly.mseventseatingprojection.exception.ResourceNotFoundException;
import com.ticketly.mseventseatingprojection.exception.UnauthorizedAccessException;
import com.ticketly.mseventseatingprojection.model.EventDocument;
import com.ticketly.mseventseatingprojection.model.ReadModelSeatStatus;
import com.ticketly.mseventseatingprojection.repository.EventAnalyticsRepository;
import com.ticketly.mseventseatingprojection.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import model.SessionStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventAnalyticsServiceImpl implements EventAnalyticsService {

    private final EventAnalyticsRepository eventAnalyticsRepository;
    private final EventOwnershipService eventOwnershipService;
    private final EventRepository eventRepository;


    @Override
    public Mono<EventAnalyticsDTO> getEventAnalytics(String eventId) {
        // Get basic event info for title
        Mono<String> eventTitleMono = eventRepository.findEventTitleById(eventId)
                .map(EventDocument::getTitle)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Event not found with ID: " + eventId)));

        // Get overall statistics using aggregation
        Mono<EventOverallStatsDTO> overallStatsMono = eventAnalyticsRepository.getEventOverallStats(eventId);

        // Get session status counts using aggregation
        Flux<SessionStatusCountDTO> sessionStatusCountsFlux = eventAnalyticsRepository.getSessionStatusCounts(eventId);

        // Get tier analytics using aggregation
        Flux<TierSalesDTO> tierAnalyticsFlux = eventAnalyticsRepository.getTierAnalytics(eventId);

        // Combine all results to build the final DTO
        return Mono.zip(
                        eventTitleMono,
                        overallStatsMono,
                        sessionStatusCountsFlux.collectMap(SessionStatusCountDTO::getStatus, SessionStatusCountDTO::getCount),
                        tierAnalyticsFlux.collectList()
                )
                .map(tuple -> {
                    String eventTitle = tuple.getT1();
                    EventOverallStatsDTO stats = tuple.getT2();
                    Map<SessionStatus, Integer> sessionStatusCounts = tuple.getT3();
                    List<TierSalesDTO> tierAnalytics = tuple.getT4();

                    // Build and return the final DTO
                    return EventAnalyticsDTO.builder()
                            .eventId(eventId)
                            .eventTitle(eventTitle)
                            .totalRevenue(stats.getTotalRevenue())
                            .averageRevenuePerTicket(stats.getAverageRevenuePerTicket())
                            .totalTicketsSold(stats.getTotalTicketsSold())
                            .totalEventCapacity(stats.getTotalEventCapacity())
                            .overallSellOutPercentage(stats.getOverallSellOutPercentage())
                            .sessionStatusBreakdown(sessionStatusCounts)
                            .salesByTier(tierAnalytics)
                            .build();
                })
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Event not found with ID: " + eventId)));
    }

    @Override
    public Mono<EventAnalyticsDTO> getEventAnalytics(String eventId, String userId) {
        return eventOwnershipService.isUserOwnerOfEvent(userId, eventId)
                .flatMap(isOwner -> {
                    if (isOwner) {
                        return getEventAnalytics(eventId);
                    } else {
                        return Mono.error(new UnauthorizedAccessException("Event analytics", eventId, userId));
                    }
                });
    }

    @Override
    public Flux<SessionSummaryDTO> getAllSessionsAnalytics(String eventId) {
        return eventAnalyticsRepository.getAllSessionsAnalytics(eventId)
                .switchIfEmpty(Flux.error(new ResourceNotFoundException("Event not found with ID: " + eventId)));
    }


    @Override
    public Flux<SessionSummaryDTO> getAllSessionsAnalytics(String eventId, String userId) {
        return eventOwnershipService.isUserOwnerOfEvent(userId, eventId)
                .flatMapMany(isOwner -> {
                    if (isOwner) {
                        return getAllSessionsAnalytics(eventId);
                    } else {
                        return Flux.error(new UnauthorizedAccessException("Event sessions analytics", eventId, userId));
                    }
                });
    }

    @Override
    public Mono<SessionAnalyticsDTO> getSessionAnalytics(String eventId, String sessionId) {
        // Get session summary (base information)
        Mono<SessionSummaryDTO> sessionSummaryMono = eventAnalyticsRepository.getSessionSummary(eventId, sessionId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Session not found with ID: " + sessionId)));

        // Get tier analytics for this specific session
        Flux<TierSalesDTO> tierAnalyticsFlux = eventAnalyticsRepository.getTierAnalytics(eventId, sessionId);

        // Get seat status counts for this session
        Flux<SeatStatusCountDTO> seatStatusCountsFlux = eventAnalyticsRepository.getSessionStatusCounts(eventId, sessionId);

        // Get block occupancy data
        Flux<BlockOccupancyDTO> blockOccupancyFlux = eventAnalyticsRepository.getBlockOccupancy(eventId, sessionId);

        // Current time for calculating time-based insights
        Instant now = Instant.now();

        // Combine all results to build the final DTO
        return Mono.zip(
                        sessionSummaryMono,
                        tierAnalyticsFlux.collectList(),
                        seatStatusCountsFlux.collectMap(SeatStatusCountDTO::getStatus, SeatStatusCountDTO::getCount),
                        blockOccupancyFlux.collectList()
                )
                .map(tuple -> {
                    SessionSummaryDTO summaryDTO = tuple.getT1();
                    List<TierSalesDTO> tierAnalytics = tuple.getT2();
                    Map<ReadModelSeatStatus, Integer> seatStatusCounts = tuple.getT3();
                    List<BlockOccupancyDTO> blockOccupancy = tuple.getT4();

                    // Calculate time-based insights
                    Duration timeUntilStart = summaryDTO.getStartTime() != null && summaryDTO.getStartTime().isAfter(now)
                            ? Duration.between(now, summaryDTO.getStartTime())
                            : Duration.ZERO;

                    Duration salesWindowDuration = summaryDTO.getSalesStartTime() != null && summaryDTO.getStartTime() != null
                            ? Duration.between(summaryDTO.getSalesStartTime(), summaryDTO.getStartTime())
                            : Duration.ZERO;

                    // Build and return the final DTO, using the builder from the parent class
                    return SessionAnalyticsDTO.builder()
                            // Copy all fields from the summary DTO
                            .sessionId(summaryDTO.getSessionId())
                            .eventId(summaryDTO.getEventId())
                            .eventTitle(summaryDTO.getEventTitle())
                            .startTime(summaryDTO.getStartTime())
                            .endTime(summaryDTO.getEndTime())
                            .salesStartTime(summaryDTO.getSalesStartTime())
                            .sessionRevenue(summaryDTO.getSessionRevenue())
                            .ticketsSold(summaryDTO.getTicketsSold())
                            .sessionStatus(summaryDTO.getSessionStatus())
                            .sessionCapacity(summaryDTO.getSessionCapacity())
                            .sellOutPercentage(summaryDTO.getSellOutPercentage())
                            .timeUntilStart(timeUntilStart)
                            .salesWindowDuration(salesWindowDuration)
                            .salesByTier(tierAnalytics)
                            .seatStatusBreakdown(seatStatusCounts)
                            .occupancyByBlock(blockOccupancy)
                            .build();
                });
    }

    @Override
    public Mono<SessionAnalyticsDTO> getSessionAnalytics(String eventId, String sessionId, String userId) {
        return eventOwnershipService.isUserOwnerOfEvent(userId, eventId)
                .flatMap(isOwner -> {
                    if (isOwner) {
                        return getSessionAnalytics(eventId, sessionId);
                    } else {
                        return Mono.error(new UnauthorizedAccessException("Session analytics", sessionId, userId));
                    }
                });
    }

    @Override
    public Flux<EventAnalyticsDTO> getBatchEventAnalytics(List<String> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return Flux.empty();
        }

        // Get basic event info for titles
        Flux<EventDocument> eventDocuments = eventRepository.findAllById(eventIds);

        return eventDocuments.collectMap(
                EventDocument::getId,
                EventDocument::getTitle
            )
            .flatMapMany(eventTitleMap -> {
                // Get overall statistics for all events in batch
                return eventAnalyticsRepository.getBatchEventOverallStats(eventIds)
                    .flatMap(stats -> {
                        String eventId = stats.get_id(); // Using _id from the aggregation
                        String eventTitle = eventTitleMap.getOrDefault(eventId, "Unknown Event");

                        // For each event, get session status counts and tier analytics
                        Mono<Map<SessionStatus, Integer>> sessionStatusMono = eventAnalyticsRepository
                            .getSessionStatusCounts(eventId)
                            .collectMap(SessionStatusCountDTO::getStatus, SessionStatusCountDTO::getCount)
                            .defaultIfEmpty(Collections.emptyMap());

                        Mono<List<TierSalesDTO>> tierAnalyticsMono = eventAnalyticsRepository
                            .getTierAnalytics(eventId)
                            .collectList()
                            .defaultIfEmpty(Collections.emptyList());

                        // Combine all data for this event
                        return Mono.zip(
                                Mono.just(stats),
                                sessionStatusMono,
                                tierAnalyticsMono
                            )
                            .map(tuple -> {
                                EventOverallStatsDTO statsData = tuple.getT1();
                                Map<SessionStatus, Integer> sessionStatusCounts = tuple.getT2();
                                List<TierSalesDTO> tierAnalytics = tuple.getT3();

                                // Build the final DTO for this event
                                return EventAnalyticsDTO.builder()
                                    .eventId(eventId)
                                    .eventTitle(eventTitle)
                                    .totalRevenue(statsData.getTotalRevenue())
                                    .averageRevenuePerTicket(statsData.getAverageRevenuePerTicket())
                                    .totalTicketsSold(statsData.getTotalTicketsSold())
                                    .totalEventCapacity(statsData.getTotalEventCapacity())
                                    .overallSellOutPercentage(statsData.getOverallSellOutPercentage())
                                    .sessionStatusBreakdown(sessionStatusCounts)
                                    .salesByTier(tierAnalytics)
                                    .build();
                            });
                    });
            });
    }

    @Override
    public Flux<EventAnalyticsDTO> getBatchEventAnalytics(List<String> eventIds, String userId) {
        if (eventIds == null || eventIds.isEmpty()) {
            return Flux.empty();
        }

        // Check ownership for all requested events
        return Flux.fromIterable(eventIds)
            .flatMap(eventId ->
                eventOwnershipService.isUserOwnerOfEvent(userId, eventId)
                    .filter(Boolean::booleanValue)
                    .map(isOwner -> eventId)
            )
            .collectList()
            .flatMapMany(authorizedEventIds -> {
                if (authorizedEventIds.isEmpty()) {
                    return Flux.empty();
                }
                return getBatchEventAnalytics(authorizedEventIds);
            });
    }
}
