package com.ticketly.mseventseatingprojection.repository;

import com.ticketly.mseventseatingprojection.model.EventDocument;
import reactor.core.publisher.Flux;

public interface TrendingRepositoryCustom {
    /**
     * Finds the top trending events with all event details except the layout data
     * which is excluded for efficiency
     * 
     * @param limit Maximum number of events to return
     * @return Flux of EventDocument objects without layout data
     */
    Flux<EventDocument> findTopTrendingEvents(int limit);
}