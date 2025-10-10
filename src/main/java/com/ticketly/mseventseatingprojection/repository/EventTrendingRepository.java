package com.ticketly.mseventseatingprojection.repository;

import com.ticketly.mseventseatingprojection.model.EventTrendingDocument;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Mono;

public interface EventTrendingRepository extends ReactiveMongoRepository<EventTrendingDocument, String> {
    
    Mono<EventTrendingDocument> findByEventId(String eventId);
    
}