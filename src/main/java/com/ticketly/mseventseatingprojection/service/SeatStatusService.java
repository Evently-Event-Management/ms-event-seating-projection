package com.ticketly.mseventseatingprojection.service;

import com.ticketly.mseventseatingprojection.model.EventDocument;
import com.ticketly.mseventseatingprojection.model.ReadModelSeatStatus;
import com.ticketly.mseventseatingprojection.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SeatStatusService {

    private final ReactiveMongoTemplate mongoTemplate;
    private final EventRepository eventRepository;

    public Mono<Void> updateSeatStatus(String sessionId, List<String> seatIds, ReadModelSeatStatus seatStatus) {
        if (seatIds == null || seatIds.isEmpty()) {
            return Mono.empty();
        }

        log.info("Updating {} seats to status {} in session {}", seatIds.size(), seatStatus, sessionId);

        return eventRepository.findEventBySessionId(sessionId)
                .flatMap(event -> {
                    String eventId = event.getId();

                    // Create query to find the event document
                    Query query = new Query(Criteria.where("_id").is(eventId));

                    // Create update for block rows seats
                    Update rowSeatsUpdate = new Update();
                    rowSeatsUpdate.set("sessions.$[session].layoutData.layout.blocks.$[].rows.$[].seats.$[seat].status", seatStatus.toString());

                    // Create update for block seats (standing blocks)
                    Update blockSeatsUpdate = new Update();
                    blockSeatsUpdate.set("sessions.$[session].layoutData.layout.blocks.$[].seats.$[seat].status", seatStatus.toString());

                    // Add array filter for session and seats
                    rowSeatsUpdate.filterArray("session.id", sessionId);
                    rowSeatsUpdate.filterArray("seat.id", new Criteria().in(seatIds));

                    blockSeatsUpdate.filterArray("session.id", sessionId);
                    blockSeatsUpdate.filterArray("seat.id", new Criteria().in(seatIds));

                    // Execute updates one after another
                    return mongoTemplate.updateMulti(query, rowSeatsUpdate, EventDocument.class)
                            .then(mongoTemplate.updateMulti(query, blockSeatsUpdate, EventDocument.class))
                            .then();
                })
                .doOnSuccess(v -> log.info("Successfully updated seat statuses for session {}", sessionId))
                .doOnError(e -> log.error("Error updating seat status in MongoDB: {}", e.getMessage()))
                .onErrorResume(e -> {
                    log.error("Failed to update seat status in MongoDB, will continue with SSE update", e);
                    return Mono.empty();
                });
    }

    public Mono<Void> updateSeatStatus(UUID sessionId, List<UUID> seatIds, ReadModelSeatStatus seatStatus) {
        List<String> seatIdStrings = seatIds.stream()
                .map(UUID::toString)
                .collect(Collectors.toList());
        return updateSeatStatus(sessionId.toString(), seatIdStrings, seatStatus);
    }
}