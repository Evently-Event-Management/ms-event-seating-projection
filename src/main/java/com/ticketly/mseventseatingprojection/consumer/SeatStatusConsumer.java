package com.ticketly.mseventseatingprojection.consumer;

import com.ticketly.mseventseatingprojection.dto.SeatStatusChangeEventDto;
import com.ticketly.mseventseatingprojection.dto.read.SeatStatusUpdateDto;
import com.ticketly.mseventseatingprojection.model.ReadModelSeatStatus;
import com.ticketly.mseventseatingprojection.service.SeatStatusService;
import com.ticketly.mseventseatingprojection.service.SseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SeatStatusConsumer {

    private final SseService sseService;
    private final SeatStatusService seatStatusService;

    /**
     * Handles Kafka events for seat status changes.
     * Processes different seat statuses (LOCKED, AVAILABLE, BOOKED) with appropriate logic.
     *
     * @param payload The seat status change event payload containing session_id, seat_ids and status.
     * @param acknowledgment Kafka acknowledgment.
     */
    @KafkaListener(topics = "ticketly.seats.status")
    public void onSeatStatusChange(@Payload SeatStatusChangeEventDto payload, Acknowledgment acknowledgment) {
        log.info("Received seat status change event to {} for session: {}", payload.status(), payload.session_id());
        try {
            // Create update DTO for SSE
            SeatStatusUpdateDto update = new SeatStatusUpdateDto(payload.seat_ids(), payload.status());

            switch (payload.status()) {
                case LOCKED -> handleLocked(payload, update, acknowledgment);
                case AVAILABLE -> handleAvailable(payload, update, acknowledgment);
                case BOOKED -> handleBooked(payload, update, acknowledgment);
                default -> {
                    log.warn("Unhandled seat status type: {}", payload.status());
                    acknowledgment.acknowledge();
                }
            }
        } catch (Exception e) {
            log.error("Error processing seat status change event for session {}: {}", payload.session_id(), e.getMessage());
            // Do not acknowledge, let Kafka retry
        }
    }

    /**
     * Handles the LOCKED status - updates MongoDB and publishes SSE event if successful.
     */
    private void handleLocked(SeatStatusChangeEventDto payload, SeatStatusUpdateDto update, Acknowledgment acknowledgment) {
        log.info("Processing LOCKED status for session: {}", payload.session_id());
        updateStatusAndPublish(payload.session_id(), payload.seat_ids(), ReadModelSeatStatus.LOCKED, update, acknowledgment);
    }

    /**
     * Handles the AVAILABLE status - updates MongoDB and publishes SSE event if successful.
     */
    private void handleAvailable(SeatStatusChangeEventDto payload, SeatStatusUpdateDto update, Acknowledgment acknowledgment) {
        log.info("Processing AVAILABLE status for session: {}", payload.session_id());
        updateStatusAndPublish(payload.session_id(), payload.seat_ids(), ReadModelSeatStatus.AVAILABLE, update, acknowledgment);
    }

    /**
     * Handles the BOOKED status - only publishes SSE event (no MongoDB update).
     */
    private void handleBooked(SeatStatusChangeEventDto payload, SeatStatusUpdateDto update, Acknowledgment acknowledgment) {
        log.info("Processing BOOKED status for session: {}", payload.session_id());
        try {
            // Only publish SSE event for booked seats, no MongoDB update as per requirement
            // This will be handled by CQRS projection
            sseService.publish(update, payload.session_id());
            log.info("Published SSE event for BOOKED seats in session: {}", payload.session_id());
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Error processing BOOKED status for session {}: {}", payload.session_id(), e.getMessage());
            // Do not acknowledge, let Kafka retry
        }
    }

    /**
     * Common method to update MongoDB and conditionally publish SSE event.
     */
    private void updateStatusAndPublish(UUID sessionId, List<UUID> seatIds, ReadModelSeatStatus status, SeatStatusUpdateDto update, Acknowledgment acknowledgment) {
        seatStatusService.updateSeatStatus(sessionId, seatIds, status)
            .doOnSuccess(success -> {
                if (success) {
                    log.info("Successfully updated seat status to {} in MongoDB", status);
                } else {
                    log.warn("Failed to update seat status to {} in MongoDB due to business rules", status);
                }
            })
            .doOnError(e -> log.error("Failed to update seat status in MongoDB: {}", e.getMessage()))
            .onErrorReturn(false)
            .flatMap(success -> {
                if (success) {
                    // Only publish SSE event if MongoDB update was successful
                    sseService.publish(update, sessionId);
                    log.info("Published SSE event for {} seats in session: {}", status, sessionId);
                } else {
                    log.warn("Skipping SSE event publication due to failed MongoDB update for session: {}", sessionId);
                }
                return Mono.just(success);
            })
            .doFinally(signalType -> acknowledgment.acknowledge())
            .subscribe();
    }
}