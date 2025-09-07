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

    /**
     * Updates the status of the specified seats for a given session.
     *
     * @param sessionId The ID of the session.
     * @param seatIds   The list of seat IDs to update.
     * @param seatStatus The new status to set for the seats.
     * @return Mono signaling completion.
     */
    public Mono<Void> updateSeatStatus(String sessionId, List<String> seatIds, ReadModelSeatStatus seatStatus) {
        if (seatIds == null || seatIds.isEmpty()) {
            return Mono.empty();
        }

        log.info("Updating {} seats to status {} in session {}", seatIds.size(), seatStatus, sessionId);

        return eventRepository.findEventBySessionId(sessionId)
                .flatMap(event -> {
                    String eventId = event.getId();

                    Query query = Query.query(Criteria.where("_id").is(eventId));

                    Update update = new Update()
                            .set("sessions.$[sess].layoutData.layout.blocks.$[].rows.$[].seats.$[seat].status", seatStatus.toString())
                            .set("sessions.$[sess].layoutData.layout.blocks.$[].seats.$[seat].status", seatStatus.toString())
                            .filterArray("sess._id", sessionId)
                            // ✅ FINAL FIX: Use the actual DB field name '_id' for the seat as well.
                            .filterArray(Criteria.where("seat._id").in(seatIds));

                    return mongoTemplate.update(EventDocument.class)
                            .inCollection("events")
                            .matching(query)
                            .apply(update)
                            .all()
                            .doOnSuccess(updateResult -> log.info(
                                    "MongoDB update result: Matched Count: {}, Modified Count: {}",
                                    updateResult.getMatchedCount(),
                                    updateResult.getModifiedCount()
                            ))
                            .then();
                })
                .doOnError(e -> log.error("Error updating seat status in MongoDB: {}", e.getMessage()))
                .onErrorResume(e -> {
                    log.error("Failed to update seat status in MongoDB, will continue with SSE update", e);
                    return Mono.empty();
                });
    }

    /**
     * Updates the status of the specified seats for a given session using UUIDs.
     *
     * @param sessionId The UUID of the session.
     * @param seatIds   The list of seat UUIDs to update.
     * @param seatStatus The new status to set for the seats.
     * @return Mono signaling completion.
     */
    public Mono<Void> updateSeatStatus(UUID sessionId, List<UUID> seatIds, ReadModelSeatStatus seatStatus) {
        List<String> seatIdStrings = seatIds.stream()
                .map(UUID::toString)
                .collect(Collectors.toList());
        return updateSeatStatus(sessionId.toString(), seatIdStrings, seatStatus);
    }
}