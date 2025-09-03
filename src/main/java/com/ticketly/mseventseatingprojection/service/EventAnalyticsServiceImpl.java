package com.ticketly.mseventseatingprojection.service;

import com.ticketly.mseventseatingprojection.dto.analytics.*;
import com.ticketly.mseventseatingprojection.exception.ResourceNotFoundException;
import com.ticketly.mseventseatingprojection.model.EventDocument;
import com.ticketly.mseventseatingprojection.model.ReadModelSeatStatus;
import com.ticketly.mseventseatingprojection.repository.EventAnalyticsRepository;
import lombok.RequiredArgsConstructor;
import model.SessionStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventAnalyticsServiceImpl implements EventAnalyticsService {

    private final EventAnalyticsRepository eventAnalyticsRepository;

    @Override
    public Mono<EventAnalyticsDTO> getEventAnalytics(String eventId) {
        return eventAnalyticsRepository.findEventWithCompleteSeatingData(eventId)
                .map(this::calculateEventAnalytics)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Event not found with ID: " + eventId)));
    }

    @Override
    public Mono<SessionAnalyticsDTO> getSessionAnalytics(String eventId, String sessionId) {
        return eventAnalyticsRepository.findSessionWithCompleteSeatingData(eventId, sessionId)
                .<SessionAnalyticsDTO>handle((event, sink) -> {
                    Optional<EventDocument.SessionInfo> sessionOpt = event.getSessions().stream()
                            .filter(session -> session.getId().equals(sessionId))
                            .findFirst();

                    if (sessionOpt.isPresent()) {
                        sink.next(calculateSessionAnalytics(event, sessionOpt.get()));
                    } else {
                        sink.error(new ResourceNotFoundException("Session not found with ID: " + sessionId));
                    }
                })
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Event not found with ID: " + eventId)));
    }

    @Override
    public Flux<SessionSummaryDTO> getAllSessionsAnalytics(String eventId) {
        return eventAnalyticsRepository.findEventWithCompleteSeatingData(eventId)
                .flatMapMany(event -> {
                    List<SessionSummaryDTO> sessionSummaries = event.getSessions().stream()
                            .map(session -> calculateSessionSummary(event, session))
                            .collect(Collectors.toList());
                    return Flux.fromIterable(sessionSummaries);
                })
                .switchIfEmpty(Flux.error(new ResourceNotFoundException("Event not found with ID: " + eventId)));
    }

    /**
     * Calculate comprehensive analytics for an entire event
     */
    private EventAnalyticsDTO calculateEventAnalytics(EventDocument event) {
        // Initialize metrics
        BigDecimal totalRevenue = BigDecimal.ZERO;
        int totalTicketsSold = 0;
        int totalEventCapacity = 0;

        // Session status counts
        Map<SessionStatus, Integer> sessionStatusCounts = new HashMap<>();

        // Tier sales tracking
        Map<String, TierSalesDTO.TierSalesDTOBuilder> tierSalesMap = new HashMap<>();

        // Process all sessions in the event
        for (EventDocument.SessionInfo session : event.getSessions()) {
            sessionStatusCounts.put(session.getStatus(), sessionStatusCounts.getOrDefault(session.getStatus(), 0) + 1);

            // Process seating data if available
            if (session.getLayoutData() != null && session.getLayoutData().getLayout() != null) {
                // Process each block in the layout
                for (EventDocument.BlockInfo block : session.getLayoutData().getLayout().getBlocks()) {
                    // Process seats in rows
                    if (block.getRows() != null) {
                        for (EventDocument.RowInfo row : block.getRows()) {
                            if (row.getSeats() != null) {
                                processSeatsList(row.getSeats(), tierSalesMap);
                                totalEventCapacity += row.getSeats().size();
                                totalTicketsSold += countBookedSeats(row.getSeats());
                                totalRevenue = totalRevenue.add(calculateRevenueFromSeats(row.getSeats()));
                            }
                        }
                    }

                    // Process seats directly in block (for standing areas)
                    if (block.getSeats() != null) {
                        processSeatsList(block.getSeats(), tierSalesMap);
                        totalEventCapacity += block.getSeats().size();
                        totalTicketsSold += countBookedSeats(block.getSeats());
                        totalRevenue = totalRevenue.add(calculateRevenueFromSeats(block.getSeats()));
                    }
                }
            }
        }

        // Calculate average revenue per ticket
        BigDecimal averageRevenuePerTicket = totalTicketsSold > 0
                ? totalRevenue.divide(BigDecimal.valueOf(totalTicketsSold), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Calculate overall sell-out percentage
        double overallSellOutPercentage = totalEventCapacity > 0
                ? (double) totalTicketsSold / totalEventCapacity * 100
                : 0.0;

        // Finalize tier sales data with percentages
        List<TierSalesDTO> salesByTier = finalizeTierSalesData(tierSalesMap, totalTicketsSold);

        // Build and return the analytics DTO
        return EventAnalyticsDTO.builder()
                .eventId(event.getId())
                .eventTitle(event.getTitle())
                .totalRevenue(totalRevenue)
                .averageRevenuePerTicket(averageRevenuePerTicket)
                .totalTicketsSold(totalTicketsSold)
                .totalEventCapacity(totalEventCapacity)
                .overallSellOutPercentage(overallSellOutPercentage)
                .sessionStatusBreakdown(sessionStatusCounts)
                .salesByTier(salesByTier)
                .build();
    }

    /**
     * Calculate analytics for a specific session
     */
    private SessionAnalyticsDTO calculateSessionAnalytics(EventDocument event, EventDocument.SessionInfo session) {
        // Initialize metrics
        BigDecimal sessionRevenue = BigDecimal.ZERO;
        int ticketsSold = 0;
        int sessionCapacity = 0;

        // Seat status tracking
        Map<ReadModelSeatStatus, Integer> seatStatusCounts = new HashMap<>();

        // Tier sales tracking
        Map<String, TierSalesDTO.TierSalesDTOBuilder> tierSalesMap = new HashMap<>();

        // Block occupancy tracking
        List<BlockOccupancyDTO> blockOccupancyList = new ArrayList<>();

        // Process seating layout if available
        if (session.getLayoutData() != null && session.getLayoutData().getLayout() != null) {
            for (EventDocument.BlockInfo block : session.getLayoutData().getLayout().getBlocks()) {
                int blockCapacity = 0;
                int blockSold = 0;

                // Process seats in rows
                if (block.getRows() != null) {
                    for (EventDocument.RowInfo row : block.getRows()) {
                        if (row.getSeats() != null) {
                            processSeatsList(row.getSeats(), tierSalesMap);
                            updateSeatStatusCounts(row.getSeats(), seatStatusCounts);

                            blockCapacity += row.getSeats().size();
                            blockSold += countBookedSeats(row.getSeats());
                            sessionRevenue = sessionRevenue.add(calculateRevenueFromSeats(row.getSeats()));
                        }
                    }
                }

                // Process seats directly in block (for standing areas)
                if (block.getSeats() != null) {
                    processSeatsList(block.getSeats(), tierSalesMap);
                    updateSeatStatusCounts(block.getSeats(), seatStatusCounts);

                    blockCapacity += block.getSeats().size();
                    blockSold += countBookedSeats(block.getSeats());
                    sessionRevenue = sessionRevenue.add(calculateRevenueFromSeats(block.getSeats()));
                }

                // Record block occupancy
                double blockOccupancyPercentage = blockCapacity > 0 ? (double) blockSold / blockCapacity * 100 : 0.0;
                blockOccupancyList.add(BlockOccupancyDTO.builder()
                        .blockId(block.getId())
                        .blockName(block.getName())
                        .blockType(block.getType())
                        .totalCapacity(blockCapacity)
                        .seatsSold(blockSold)
                        .occupancyPercentage(blockOccupancyPercentage)
                        .build());

                // Add to session totals
                sessionCapacity += blockCapacity;
                ticketsSold += blockSold;
            }
        }

        // Calculate sell-out percentage
        double sellOutPercentage = sessionCapacity > 0 ? (double) ticketsSold / sessionCapacity * 100 : 0.0;

        // Calculate time-based insights
        Instant now = Instant.now();
        Duration timeUntilStart = Duration.between(now, session.getStartTime());
        Duration salesWindowDuration = session.getSalesStartTime() != null
                ? Duration.between(session.getSalesStartTime(), now)
                : Duration.ZERO;

        // Finalize tier sales data with percentages
        List<TierSalesDTO> salesByTier = finalizeTierSalesData(tierSalesMap, ticketsSold);

        // Build and return the session analytics DTO
        return SessionAnalyticsDTO.builder()
                .sessionId(session.getId())
                .eventId(event.getId())
                .eventTitle(event.getTitle())
                .startTime(session.getStartTime())
                .endTime(session.getEndTime())
                .timeUntilStart(timeUntilStart)
                .salesWindowDuration(salesWindowDuration)
                .sessionRevenue(sessionRevenue)
                .ticketsSold(ticketsSold)
                .sessionCapacity(sessionCapacity)
                .sellOutPercentage(sellOutPercentage)
                .salesByTier(salesByTier)
                .sessionStatus(session.getStatus())
                .seatStatusBreakdown(seatStatusCounts)
                .occupancyByBlock(blockOccupancyList)
                .build();
    }

    /**
     * Calculate basic analytics summary for a session
     */
    private SessionSummaryDTO calculateSessionSummary(EventDocument event, EventDocument.SessionInfo session) {
        // Initialize metrics
        BigDecimal sessionRevenue = BigDecimal.ZERO;
        int ticketsSold = 0;
        int sessionCapacity = 0;

        // Process seating layout if available
        if (session.getLayoutData() != null && session.getLayoutData().getLayout() != null) {
            for (EventDocument.BlockInfo block : session.getLayoutData().getLayout().getBlocks()) {
                // Process seats in rows
                if (block.getRows() != null) {
                    for (EventDocument.RowInfo row : block.getRows()) {
                        if (row.getSeats() != null) {
                            sessionCapacity += row.getSeats().size();
                            ticketsSold += countBookedSeats(row.getSeats());
                            sessionRevenue = sessionRevenue.add(calculateRevenueFromSeats(row.getSeats()));
                        }
                    }
                }

                // Process seats directly in block (for standing areas)
                if (block.getSeats() != null) {
                    sessionCapacity += block.getSeats().size();
                    ticketsSold += countBookedSeats(block.getSeats());
                    sessionRevenue = sessionRevenue.add(calculateRevenueFromSeats(block.getSeats()));
                }
            }
        }

        // Calculate sell-out percentage
        double sellOutPercentage = sessionCapacity > 0 ? (double) ticketsSold / sessionCapacity * 100 : 0.0;

        // Build and return the session summary DTO
        return SessionSummaryDTO.builder()
                .sessionId(session.getId())
                .eventId(event.getId())
                .eventTitle(event.getTitle())
                .startTime(session.getStartTime())
                .endTime(session.getEndTime())
                .sessionRevenue(sessionRevenue)
                .ticketsSold(ticketsSold)
                .sessionStatus(session.getStatus())
                .sessionCapacity(sessionCapacity)
                .sellOutPercentage(sellOutPercentage)
                .build();
    }

    /**
     * Process a list of seats and update the tier sales tracking map
     */
    private void processSeatsList(List<EventDocument.SeatInfo> seats,
                                  Map<String, TierSalesDTO.TierSalesDTOBuilder> tierSalesMap) {
        for (EventDocument.SeatInfo seat : seats) {
            if (seat.getStatus() == ReadModelSeatStatus.BOOKED && seat.getTier() != null) {
                String tierId = seat.getTier().getId();
                TierSalesDTO.TierSalesDTOBuilder tierBuilder = tierSalesMap.computeIfAbsent(
                        tierId,
                        id -> TierSalesDTO.builder()
                                .tierId(id)
                                .tierName(seat.getTier().getName())
                                .tierColor(seat.getTier().getColor())
                                .ticketsSold(0)
                                .totalRevenue(BigDecimal.ZERO)
                );

                // Update the builder with this seat's data
                tierBuilder.ticketsSold(tierBuilder.build().getTicketsSold() + 1);
                tierBuilder.totalRevenue(
                        tierBuilder.build().getTotalRevenue().add(seat.getTier().getPrice())
                );
            }
        }
    }

    /**
     * Update the counts of seats by status
     */
    private void updateSeatStatusCounts(List<EventDocument.SeatInfo> seats, Map<ReadModelSeatStatus, Integer> statusCounts) {
        for (EventDocument.SeatInfo seat : seats) {
            statusCounts.put(seat.getStatus(), statusCounts.getOrDefault(seat.getStatus(), 0) + 1);
        }
    }

    /**
     * Count the number of booked seats in a list
     */
    private int countBookedSeats(List<EventDocument.SeatInfo> seats) {
        return (int) seats.stream()
                .filter(seat -> seat.getStatus() == ReadModelSeatStatus.BOOKED)
                .count();
    }

    /**
     * Calculate the total revenue from a list of seats
     */
    private BigDecimal calculateRevenueFromSeats(List<EventDocument.SeatInfo> seats) {
        return seats.stream()
                .filter(seat -> seat.getStatus() == ReadModelSeatStatus.BOOKED && seat.getTier() != null)
                .map(seat -> seat.getTier().getPrice())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Finalize the tier sales data by calculating percentages
     */
    private List<TierSalesDTO> finalizeTierSalesData(
            Map<String, TierSalesDTO.TierSalesDTOBuilder> tierSalesMap, int totalTicketsSold) {

        return tierSalesMap.values().stream()
                .map(builder -> {
                    TierSalesDTO dto = builder.build();
                    double percentage = totalTicketsSold > 0
                            ? (double) dto.getTicketsSold() / totalTicketsSold * 100
                            : 0.0;
                    return TierSalesDTO.builder()
                            .tierId(dto.getTierId())
                            .tierName(dto.getTierName())
                            .tierColor(dto.getTierColor())
                            .ticketsSold(dto.getTicketsSold())
                            .totalRevenue(dto.getTotalRevenue())
                            .percentageOfTotalSales(percentage)
                            .build();
                })
                .collect(Collectors.toList());
    }
}
