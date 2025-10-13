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
    public Mono<SessionSummaryDTO> getSessionSummary(String eventId, String sessionId) {
        return eventAnalyticsRepository.getSessionSummary(eventId, sessionId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Session not found with ID: " + sessionId)));
    }

    @Override
    public Mono<SessionSummaryDTO> getSessionSummary(String eventId, String sessionId, String userId) {
        return eventOwnershipService.isUserOwnerOfEvent(userId, eventId)
                .flatMap(isOwner -> {
                    if (isOwner) {
                        return getSessionSummary(eventId, sessionId);
                    } else {
                        return Mono.error(new UnauthorizedAccessException("Session summary", sessionId, userId));
                    }
                });
    }
}
