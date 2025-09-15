package com.ticketly.mseventseatingprojection.service;

import com.ticketly.mseventseatingprojection.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class EventOwnershipService {

    private final EventRepository eventRepository;

    /**
     * Checks if the given user is the owner of the specified event.
     *
     * @param userId  The ID of the user to check.
     * @param eventId The ID of the event to check ownership for.
     * @return A Mono emitting true if the user is the owner, false otherwise.
     */
    public Mono<Boolean> isUserOwnerOfEvent(String userId, String eventId) {
        return eventRepository.findOwnerIdByEventId(eventId)
                .map(event -> {
                    // Check if the organization and its userId exist and match the provided userId
                    return event.getOrganization() != null &&
                            userId.equals(event.getOrganization().getUserId());
                })
                .defaultIfEmpty(false); // If the event is not found, the user is not the owner.
    }
}