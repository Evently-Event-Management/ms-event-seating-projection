package com.ticketly.mseventseatingprojection.consumer;

import com.ticketly.mseventseatingprojection.dto.SeatStatusChangeEventDto;
import com.ticketly.mseventseatingprojection.dto.read.SeatStatusUpdateDto;
import com.ticketly.mseventseatingprojection.model.ReadModelSeatStatus;
import com.ticketly.mseventseatingprojection.service.SseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SeatStatusConsumer {

    private final SseService sseService;

    @KafkaListener(topics = "ticketly.seats.locked")
    public void onSeatsLocked(@Payload SeatStatusChangeEventDto payload, Acknowledgment acknowledgment) {
        log.info("Received SeatsLocked event for session: {}", payload.sessionId());
        try {
            SeatStatusUpdateDto update = new SeatStatusUpdateDto(payload.seatIds(), ReadModelSeatStatus.LOCKED);
            sseService.publish(update, payload.sessionId());
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Error processing SeatsLocked event for session {}: {}", payload.sessionId(), e.getMessage());
            // Do not acknowledge, let Kafka retry
        }
    }

    @KafkaListener(topics = "ticketly.seats.released")
    public void onSeatsReleased(@Payload SeatStatusChangeEventDto payload, Acknowledgment acknowledgment) {
        log.info("Received SeatsReleased event for session: {}", payload.sessionId());
        try {
            SeatStatusUpdateDto update = new SeatStatusUpdateDto(payload.seatIds(), ReadModelSeatStatus.AVAILABLE);
            sseService.publish(update, payload.sessionId());
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Error processing SeatsReleased event for session {}: {}", payload.sessionId(), e.getMessage());
            // Do not acknowledge, let Kafka retry
        }
    }

    @KafkaListener(topics = "ticketly.seats.booked")
    public void onSeatsBooked(@Payload SeatStatusChangeEventDto payload, Acknowledgment acknowledgment) {
        log.info("Received SeatsBooked event for session: {}", payload.sessionId());
        try {
            SeatStatusUpdateDto update = new SeatStatusUpdateDto(payload.seatIds(), ReadModelSeatStatus.BOOKED);
            sseService.publish(update, payload.sessionId());
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Error processing SeatsBooked event for session {}: {}", payload.sessionId(), e.getMessage());
            // Do not acknowledge, let Kafka retry
        }
    }
}