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

@Service
@RequiredArgsConstructor
@Slf4j
public class SeatStatusConsumer {

    private final SseService sseService;
    private final SeatStatusService seatStatusService;

    /**
     * Handles Kafka events for seats being locked.
     * Updates seat status in MongoDB and publishes SSE event.
     *
     * @param payload The seat status change event payload.
     * @param acknowledgment Kafka acknowledgment.
     */
    @KafkaListener(topics = "ticketly.seats.locked")
    public void onSeatsLocked(@Payload SeatStatusChangeEventDto payload, Acknowledgment acknowledgment) {
        log.info("Received SeatsLocked event for session: {}", payload.session_id());
        try {
            // Create update DTO for SSE
            SeatStatusUpdateDto update = new SeatStatusUpdateDto(payload.seat_ids(), ReadModelSeatStatus.LOCKED);

            // Update MongoDB first, then publish SSE event
            seatStatusService.updateSeatStatus(payload.session_id(), payload.seat_ids(), ReadModelSeatStatus.LOCKED)
                    .doOnSuccess(v -> log.info("Successfully updated seat status to LOCKED in MongoDB"))
                    .doOnError(e -> log.error("Failed to update seat status in MongoDB: {}", e.getMessage()))
                    .onErrorResume(e -> Mono.empty()) // Continue with SSE even if MongoDB update fails
                    .then(Mono.fromRunnable(() -> {
                        // Publish SSE event
                        sseService.publish(update, payload.session_id());
                        acknowledgment.acknowledge();
                    }))
                    .subscribe();
        } catch (Exception e) {
            log.error("Error processing SeatsLocked event for session {}: {}", payload.session_id(), e.getMessage());
            // Do not acknowledge, let Kafka retry
        }
    }

    /**
     * Handles Kafka events for seats being released.
     * Updates seat status in MongoDB and publishes SSE event.
     *
     * @param payload The seat status change event payload.
     * @param acknowledgment Kafka acknowledgment.
     */
    @KafkaListener(topics = "ticketly.seats.released")
    public void onSeatsReleased(@Payload SeatStatusChangeEventDto payload, Acknowledgment acknowledgment) {
        log.info("Received SeatsReleased event for session: {}", payload.session_id());
        try {
            // Create update DTO for SSE
            SeatStatusUpdateDto update = new SeatStatusUpdateDto(payload.seat_ids(), ReadModelSeatStatus.AVAILABLE);

            // Update MongoDB first, then publish SSE event
            seatStatusService.updateSeatStatus(payload.session_id(), payload.seat_ids(), ReadModelSeatStatus.AVAILABLE)
                    .doOnSuccess(v -> log.info("Successfully updated seat status to AVAILABLE in MongoDB"))
                    .doOnError(e -> log.error("Failed to update seat status in MongoDB: {}", e.getMessage()))
                    .onErrorResume(e -> Mono.empty()) // Continue with SSE even if MongoDB update fails
                    .then(Mono.fromRunnable(() -> {
                        // Publish SSE event
                        sseService.publish(update, payload.session_id());
                        acknowledgment.acknowledge();
                    }))
                    .subscribe();
        } catch (Exception e) {
            log.error("Error processing SeatsReleased event for session {}: {}", payload.session_id(), e.getMessage());
            // Do not acknowledge, let Kafka retry
        }
    }

    /**
     * Handles Kafka events for seats being booked.
     * Publishes SSE event for booked seats (no MongoDB update).
     *
     * @param payload The seat status change event payload.
     * @param acknowledgment Kafka acknowledgment.
     */
    @KafkaListener(topics = "ticketly.seats.booked")
    public void onSeatsBooked(@Payload SeatStatusChangeEventDto payload, Acknowledgment acknowledgment) {
        log.info("Received SeatsBooked event for session: {}", payload.session_id());
        try {
            // Only publish SSE event for booked seats, no MongoDB update as per requirement
            // This will be handled by CQRS projection
            SeatStatusUpdateDto update = new SeatStatusUpdateDto(payload.seat_ids(), ReadModelSeatStatus.BOOKED);
            sseService.publish(update, payload.session_id());
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Error processing SeatsBooked event for session {}: {}", payload.session_id(), e.getMessage());
            // Do not acknowledge, let Kafka retry
        }
    }
}