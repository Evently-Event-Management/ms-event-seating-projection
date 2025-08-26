package com.ticketly.mseventseatingprojection.service;

import com.ticketly.mseventseatingprojection.dto.read.SeatStatusUpdateDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class SseService {

    // A thread-safe map to hold the sinks for each active session.
    // Key: Session UUID as String
    // Value: A sink that can broadcast events to all subscribers for that session.
    private final Map<String, Sinks.Many<ServerSentEvent<SeatStatusUpdateDto>>> sinks = new ConcurrentHashMap<>();

    /**
     * Registers a new client to receive SSE updates for a specific session.
     *
     * @param sessionId The UUID of the session the client is subscribing to.
     * @return A Flux of ServerSentEvent that the client will listen to.
     */
    public Flux<ServerSentEvent<SeatStatusUpdateDto>> register(UUID sessionId) {
        String sessionIdStr = sessionId.toString();
        log.info("Registering new SSE client for session: {}", sessionIdStr);

        // computeIfAbsent ensures that we create a new sink only if one doesn't already exist for this session.
        // Sinks.many().multicast().onBackpressureBuffer() is ideal for broadcasting to multiple subscribers.
        Sinks.Many<ServerSentEvent<SeatStatusUpdateDto>> sink = sinks.computeIfAbsent(
                sessionIdStr,
                id -> Sinks.many().multicast().onBackpressureBuffer()
        );

        // When a client disconnects (doOnCancel) or the stream terminates, we check if we should remove the sink.
        return sink.asFlux()
                .doOnCancel(() -> handleDisconnect(sessionIdStr));
    }

    /**
     * Publishes a seat status update to all clients subscribed to a specific session.
     * This method is called by the Kafka consumer.
     *
     * @param update The seat status update DTO.
     */
    public void publish(SeatStatusUpdateDto update, UUID sessionId) {
        String sessionIdStr = sessionId.toString();
        Sinks.Many<ServerSentEvent<SeatStatusUpdateDto>> sink = sinks.get(sessionIdStr);

        if (sink == null) {
            log.debug("No active SSE clients for session {}. Ignoring event.", sessionIdStr);
            return;
        }

        log.info("Publishing {} event for session {} to {} subscribers. Seats: {}",
                update.status(), sessionIdStr, sink.currentSubscriberCount(), update.seatIds());

        // Create the ServerSentEvent object
        ServerSentEvent<SeatStatusUpdateDto> sseEvent = ServerSentEvent
                .<SeatStatusUpdateDto>builder()
                .event(update.status().name()) // Event name will be "LOCKED" or "AVAILABLE"
                .data(update)
                .build();

        // Emit the event. tryEmitNext is non-blocking.
        Sinks.EmitResult result = sink.tryEmitNext(sseEvent);

        if (result.isFailure()) {
            log.warn("Failed to emit SSE event for session {}. Reason: {}", sessionIdStr, result);
        }
    }

    /**
     * Handles client disconnection.
     */
    private void handleDisconnect(String sessionId) {
        Sinks.Many<?> sink = sinks.get(sessionId);
        if (sink != null && sink.currentSubscriberCount() == 0) {
            // If this was the last subscriber, remove the sink to free up resources.
            sinks.remove(sessionId);
            log.info("Last client for session {} disconnected. Removing sink.", sessionId);
        } else if (sink != null) {
            log.debug("Client for session {} disconnected. {} subscribers remaining.", sessionId, sink.currentSubscriberCount());
        }
    }
}