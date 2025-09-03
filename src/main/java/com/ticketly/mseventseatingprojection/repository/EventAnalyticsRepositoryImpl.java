package com.ticketly.mseventseatingprojection.repository;

import com.ticketly.mseventseatingprojection.model.EventDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
@RequiredArgsConstructor
public class EventAnalyticsRepositoryImpl implements EventAnalyticsRepository {

    private final ReactiveMongoTemplate reactiveMongoTemplate;

    @Override
    public Mono<EventDocument> findEventWithCompleteSeatingData(String eventId) {
        Query query = new Query(Criteria.where("id").is(eventId));
        return reactiveMongoTemplate.findOne(query, EventDocument.class);
    }

    @Override
    public Mono<EventDocument> findSessionWithCompleteSeatingData(String eventId, String sessionId) {
        Query query = new Query(
                Criteria.where("id").is(eventId)
                        .and("sessions.id").is(sessionId)
        );
        return reactiveMongoTemplate.findOne(query, EventDocument.class);
    }
}
