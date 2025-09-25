package com.ticketly.mseventseatingprojection.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketly.mseventseatingprojection.dto.*;
import com.ticketly.mseventseatingprojection.repository.EventReadRepositoryCustom;
import com.ticketly.mseventseatingprojection.repository.EventRepository;
import com.ticketly.mseventseatingprojection.service.EventProjectionClient;
import com.ticketly.mseventseatingprojection.service.ProjectorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import model.SessionStatus;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class DebeziumEventConsumer {

    private final ProjectorService projectorService;
    private final EventRepository eventReadRepository;
    private final ObjectMapper objectMapper;
    private final EventReadRepositoryCustom eventReadRepositoryCustom;

    // ✅ A single listener for all Debezium topics
    @KafkaListener(topics = {
            "dbz.ticketly.public.events",
            "dbz.ticketly.public.event_sessions",
            "dbz.ticketly.public.session_seating_maps",
            "dbz.ticketly.public.organizations",
            "dbz.ticketly.public.categories",
            "dbz.ticketly.public.event_cover_photos",
            "dbz.ticketly.public.discounts",
            "dbz.ticketly.public.discount_tiers",
            "dbz.ticketly.public.discount_sessions"
    },
            containerFactory = "debeziumListenerContainerFactory"
    )
    public void onDebeziumEvent(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) throws Exception {
        String topic = record.topic();
        String payload = record.value();

        log.debug("Received message on topic: {}", topic);
        log.trace("Payload (truncated): {}", payload == null ? "null" : payload.length() > 200 ? payload.substring(0, 200) + "..." : payload);
        if (payload == null || payload.isBlank()) {
            log.debug("Received tombstone record on topic {}. Ignoring.", topic);
            acknowledgment.acknowledge(); // Safe to acknowledge empty messages
            return;
        }

        try {
            CompletableFuture<?> processingFuture = new CompletableFuture<>();

            // Use the topic name to decide which logic to execute
            if (topic.endsWith(".events")) {
                log.debug("Dispatching to processEventChange for topic {}", topic);
                processEventChange(payload, processingFuture);
            } else if (topic.endsWith(".event_sessions")) {
                log.debug("Dispatching to processSessionChange for topic {}", topic);
                processSessionChange(payload, processingFuture);
            } else if (topic.endsWith(".session_seating_maps")) {
                log.debug("Dispatching to processSeatingMapChange for topic {}", topic);
                processSeatingMapChange(payload, processingFuture);
            } else if (topic.endsWith(".organizations")) {
                log.debug("Dispatching to processOrganizationChange for topic {}", topic);
                processOrganizationChange(payload, processingFuture);
            } else if (topic.endsWith(".categories")) {
                log.debug("Dispatching to processCategoryChange for topic {}", topic);
                processCategoryChange(payload, processingFuture);
            } else if (topic.endsWith(".event_cover_photos")) {
                log.debug("Dispatching to processCoverPhotoChange for topic {}", topic);
                processCoverPhotoChange(payload, processingFuture);
            } else if (topic.endsWith(".discounts")) {
                log.debug("Dispatching to processDiscountMetadataChange for topic {}", topic);
                processDiscountMetadataChange(payload, processingFuture); // ✅ RENAMED
            } else if (topic.endsWith(".discount_tiers") || topic.endsWith(".discount_sessions")) {
                log.debug("Dispatching to processDiscountRelationshipChange for topic {}", topic);
                processDiscountRelationshipChange(payload, processingFuture);
            } else {
                log.warn("Unhandled topic: {}", topic);
                acknowledgment.acknowledge(); // Safe to acknowledge for unhandled topics
                return;
            }

            // Wait for processing to complete before acknowledging
            try {
                processingFuture.get(30, TimeUnit.SECONDS);
                log.info("Processing completed successfully for message on topic: {}", topic);
                acknowledgment.acknowledge();
            } catch (Exception e) {
                log.error("Error while waiting for processing to complete for topic {}: {}", topic, e.getMessage());
                // Do not acknowledge - let Kafka retry
                throw new RuntimeException("Failed to process message", e);
            }

        } catch (Exception e) {
            // For parsing errors, we can acknowledge since retrying won't help
            if (e instanceof IllegalArgumentException) {
                log.error("Error parsing Debezium event from topic {}: {}. Acknowledging message as retrying won't help.",
                        topic, payload, e);
                acknowledgment.acknowledge();
            } else {
                log.error("Error processing Debezium event from topic {}: {}", topic, payload, e);
                // Do not acknowledge - let Kafka retry
                throw e;
            }
        }
    }

    private void processEventChange(String payload, CompletableFuture<?> future) throws Exception {
        JsonNode message = objectMapper.readTree(payload).path("payload");
        String operation = message.path("op").asText();
        log.debug("processEventChange - operation: {}", operation);

        if ("d".equals(operation)) {
            UUID eventId = UUID.fromString(message.path("before").path("id").asText());
            log.info("Event delete detected for id: {}. Deleting projection.", eventId);
            projectorService.deleteEvent(eventId)
                    .doOnSuccess(v -> {
                        log.debug("deleteEvent completed for id: {}", eventId);
                        future.complete(null);
                    })
                    .doOnError(e -> handleProjectionError(e, future))
                    .subscribe();
            return;
        }

        EventChangePayload eventChange = objectMapper.treeToValue(message.path("after"), EventChangePayload.class);
        log.debug("Event after payload id={} status={}", eventChange.getId(), eventChange.getStatus());
        if ("APPROVED".equals(eventChange.getStatus()) || "COMPLETED".equals(eventChange.getStatus())) {
            log.info("Projecting full event id={} status={}", eventChange.getId(), eventChange.getStatus());
            projectorService.projectFullEvent(eventChange.getId())
                    .doOnSuccess(v -> {
                        log.debug("projectFullEvent completed for id: {}", eventChange.getId());
                        future.complete(null);
                    })
                    .doOnError(e -> handleProjectionError(e, future))
                    .subscribe();
        } else {
            log.info("Removing projection for event id={} due to status={}", eventChange.getId(), eventChange.getStatus());
            projectorService.deleteEvent(eventChange.getId())
                    .doOnSuccess(v -> {
                        log.debug("deleteEvent completed for id: {}", eventChange.getId());
                        future.complete(null);
                    })
                    .doOnError(e -> handleProjectionError(e, future))
                    .subscribe();
        }
    }

    private void processSessionChange(String payload, CompletableFuture<?> future) throws Exception {
        JsonNode message = objectMapper.readTree(payload).path("payload");
        String operation = message.path("op").asText();
        log.debug("processSessionChange - operation: {}", operation);
        if ("d".equals(operation)) {
            log.debug("Session delete detected. Nothing to project.");
            future.complete(null); // Nothing to do for deletes
            return;
        }

        SessionChangePayload sessionChange = objectMapper.treeToValue(message.path("after"), SessionChangePayload.class);
        log.debug("SessionChange payload eventId={} sessionId={}", sessionChange.getEventId(), sessionChange.getId());
        eventReadRepository.existsById(sessionChange.getEventId().toString())
                .flatMap(exists -> {
                    if (exists) {
                        log.info("Projecting session update for eventId={} sessionId={}", sessionChange.getEventId(), sessionChange.getId());
                        return projectorService.projectSessionUpdate(sessionChange.getEventId(), sessionChange.getId())
                                .doOnSuccess(v -> {
                                    log.debug("projectSessionUpdate completed for eventId={} sessionId={}", sessionChange.getEventId(), sessionChange.getId());
                                    future.complete(null);
                                })
                                .doOnError(e -> handleProjectionError(e, future))
                                .then(Mono.empty());
                    } else {
                        log.debug("Event not present in read model for eventId={}. Skipping session projection.", sessionChange.getEventId());
                        // No event exists, we can safely complete the future
                        future.complete(null);
                        return Mono.empty();
                    }
                })
                .subscribe();
    }

    private void processSeatingMapChange(String payload, CompletableFuture<?> future) throws Exception {
        JsonNode message = objectMapper.readTree(payload).path("payload");
        String operation = message.path("op").asText();
        log.debug("processSeatingMapChange - operation: {}", operation);

        if ("c".equals(operation) || "d".equals(operation)) {
            log.debug("Ignoring seating map create/delete operation: {}", operation);
            future.complete(null);
            return;
        }

        SeatingMapChangePayload mapChange = objectMapper.treeToValue(message.path("after"), SeatingMapChangePayload.class);
        String sessionIdStr = mapChange.getSessionId().toString();
        log.debug("SeatingMapChange sessionId={} lookup starting", sessionIdStr);

        // ++ CHANGE: Call the new, efficient repository method ++
        eventReadRepositoryCustom.findSessionStatusById(sessionIdStr)
                .publishOn(Schedulers.boundedElastic())
                .doOnNext(sessionStatusInfo -> {
                    // The repository now returns only the data we need
                    log.debug("Found session status info for sessionId={}: {}", sessionIdStr, sessionStatusInfo);

                    String eventId = sessionStatusInfo.getId();
                    SessionStatus status = sessionStatusInfo.getSessionStatus();
                    UUID sessionId = mapChange.getSessionId();

                    Mono<Void> projectionMono;
                    if (SessionStatus.ON_SALE.equals(status)) {
                        log.info("Applying seating map patch for eventId={} sessionId={}", eventId, sessionIdStr);
                        projectionMono = projectorService.projectSeatingMapPatch(
                                UUID.fromString(eventId),
                                sessionId,
                                mapChange.getLayoutData());
                    } else if (SessionStatus.SCHEDULED.equals(status)) {
                        log.info("Session scheduled; projecting session update for eventId={} sessionId={}", eventId, sessionIdStr);
                        projectionMono = projectorService.projectSessionUpdate(
                                UUID.fromString(eventId),
                                sessionId);
                    } else {
                        log.debug("Session status '{}' not relevant for seating changes. Skipping.", status);
                        future.complete(null);
                        return;
                    }

                    projectionMono
                            .doOnSuccess(v -> {
                                log.debug("Seating projection completed for eventId={} sessionId={}", eventId, sessionIdStr);
                                future.complete(null);
                            })
                            .doOnError(e -> handleProjectionError(e, future))
                            .subscribe();
                })
                .doFinally(signalType -> {
                    // If the mono was empty (no session found) or on completion, ensure the future is completed
                    if (!future.isDone()) {
                        log.debug("Finalizing seating map processing for sessionId={}. Completing future.", sessionIdStr);
                        future.complete(null);
                    }
                })
                .subscribe();
    }

    private void processOrganizationChange(String payload, CompletableFuture<?> future) throws Exception {
        JsonNode message = objectMapper.readTree(payload).path("payload");
        String operation = message.path("op").asText();
        log.debug("processOrganizationChange - operation: {}", operation);

        if ("d".equals(operation)) {
            String orgId = message.path("before").path("id").asText();
            log.info("Organization delete detected for id={}. Deleting projection.", orgId);
            projectorService.deleteOrganization(orgId)
                    .doOnSuccess(v -> {
                        log.debug("deleteOrganization completed for id: {}", orgId);
                        future.complete(null);
                    })
                    .doOnError(e -> handleProjectionError(e, future))
                    .subscribe();
            return;
        }

        OrganizationChangePayload orgChange = objectMapper.treeToValue(message.path("after"), OrganizationChangePayload.class);
        log.info("Projecting organization change for id={}", orgChange.getId());
        projectorService.projectOrganizationChange(orgChange)
                .doOnSuccess(v -> {
                    log.debug("projectOrganizationChange completed for id: {}", orgChange.getId());
                    future.complete(null);
                })
                .doOnError(e -> handleProjectionError(e, future))
                .subscribe();
    }

    private void processCategoryChange(String payload, CompletableFuture<?> future) throws Exception {
        JsonNode message = objectMapper.readTree(payload).path("payload");
        String operation = message.path("op").asText();
        log.debug("processCategoryChange - operation: {}", operation);

        if ("d".equals(operation)) {
            String catId = message.path("before").path("id").asText();
            log.info("Category delete detected for id={}. Deleting projection.", catId);
            projectorService.deleteCategory(catId)
                    .doOnSuccess(v -> {
                        log.debug("deleteCategory completed for id: {}", catId);
                        future.complete(null);
                    })
                    .doOnError(e -> handleProjectionError(e, future))
                    .subscribe();
            return;
        }

        CategoryChangePayload catChange = objectMapper.treeToValue(message.path("after"), CategoryChangePayload.class);
        log.info("Projecting category change for id={}", catChange.getId());
        projectorService.projectCategoryChange(catChange.getId())
                .doOnSuccess(v -> {
                    log.debug("projectCategoryChange completed for id: {}", catChange.getId());
                    future.complete(null);
                })
                .doOnError(e -> handleProjectionError(e, future))
                .subscribe();
    }

    private void processCoverPhotoChange(String payload, CompletableFuture<?> future) throws Exception {
        JsonNode message = objectMapper.readTree(payload).path("payload");
        String operation = message.path("op").asText();

        if ("u".equals(operation)) {
            future.complete(null); // Ignore updates, not relevant for this table
            return;
        }

        // Determine which part of the payload to use based on the operation
        JsonNode dataNode = "d".equals(operation) ? message.path("before") : message.path("after");
        if (dataNode.isMissingNode()) {
            future.complete(null);
            return;
        }

        CoverPhotoChangePayload photoChange = objectMapper.treeToValue(dataNode, CoverPhotoChangePayload.class);

        // ✅ CRITICAL: Only process the change if the parent event already exists in the read model.
        // This prevents race conditions during initial event creation.
        eventReadRepository.existsById(photoChange.getEventId().toString())
                .flatMap(exists -> {
                    if (exists) {
                        if ("c".equals(operation)) {
                            log.info("Projecting cover photo addition for event ID: {}", photoChange.getEventId());
                            return projectorService.projectCoverPhotoAdded(photoChange.getEventId(), photoChange.getPhotoUrl())
                                    .doOnSuccess(v -> future.complete(null))
                                    .doOnError(e -> handleProjectionError(e, future))
                                    .then(Mono.empty());
                        } else if ("d".equals(operation)) {
                            log.info("Projecting cover photo removal for event ID: {}", photoChange.getEventId());
                            return projectorService.projectCoverPhotoRemoved(photoChange.getEventId(), photoChange.getPhotoUrl())
                                    .doOnSuccess(v -> future.complete(null))
                                    .doOnError(e -> handleProjectionError(e, future))
                                    .then(Mono.empty());
                        } else {
                            log.warn("Unhandled operation '{}' for cover photo change. Ignoring.", operation);
                            future.complete(null);
                            return Mono.empty();
                        }
                    } else {
                        // No event exists, we can safely complete the future
                        future.complete(null);
                        return Mono.empty();
                    }
                })
                .subscribe();
    }

    private void processDiscountMetadataChange(String payload, CompletableFuture<?> future) throws Exception {
        JsonNode message = objectMapper.readTree(payload).path("payload");
        String operation = message.path("op").asText();

        // For Deletion, we get IDs from the 'before' payload
        if ("d".equals(operation)) {
            DiscountMetadataChangePayload discountChange = objectMapper.treeToValue(message.path("before"), DiscountMetadataChangePayload.class);
            projectorService.projectDiscountDeletion(discountChange.getEventId(), discountChange.getId())
                    .doOnSuccess(v -> future.complete(null))
                    .doOnError(e -> handleProjectionError(e, future))
                    .subscribe();
            return;
        }

        // For Create and Update, we use the 'after' payload
        DiscountMetadataChangePayload discountChange = objectMapper.treeToValue(message.path("after"), DiscountMetadataChangePayload.class);

        eventReadRepository.existsById(discountChange.getEventId().toString())
                .flatMap(exists -> {
                    if (!exists) {
                        future.complete(null); // Parent event doesn't exist, skip.
                        return Mono.empty();
                    }

                    // HYBRID LOGIC
                    Mono<Void> action;
                    if ("c".equals(operation)) {
                        log.info("Projecting full discount on create: {}", discountChange.getId());
                        action = projectorService.projectFullDiscount(discountChange.getEventId(), discountChange.getId());
                    } else {
                        log.info("Patching discount on update: {}", discountChange.getId());
                        action = projectorService.patchDiscount(discountChange);
                    }

                    return action
                            .doOnSuccess(v -> future.complete(null))
                            .doOnError(e -> handleProjectionError(e, future));
                }).subscribe();
    }

    /**
     * STRATEGY 1: Handles structural changes from join tables ('discount_tiers', 'discount_sessions').
     * This triggers a full "Signal and Fetch" of the affected discount to ensure consistency.
     */
    private void processDiscountRelationshipChange(String payload, CompletableFuture<?> future) throws Exception {
        JsonNode message = objectMapper.readTree(payload).path("payload");
        JsonNode dataNode = message.path("op").asText().equals("d") ? message.path("before") : message.path("after");
        if (dataNode.isMissingNode()) {
            future.complete(null);
            return;
        }

        DiscountJoinTablePayload joinChange = objectMapper.treeToValue(dataNode, DiscountJoinTablePayload.class);
        UUID discountId = joinChange.getDiscountId();

        // We need to find the parent event ID from the discount ID.
        eventReadRepository.findEventIdByDiscountId(discountId.toString())
                .flatMap(eventDoc -> {
                    if (eventDoc == null) {
                        log.debug("Parent event not found for discount {}. Skipping projection.", discountId);
                        future.complete(null);
                        return Mono.empty();
                    }

                    log.info("Projecting full discount {} due to relationship change.", discountId);
                    // Trigger the full re-projection of this single discount
                    return projectorService.projectFullDiscount(UUID.fromString(eventDoc.getId()), discountId)
                            .doOnSuccess(v -> future.complete(null))
                            .doOnError(e -> handleProjectionError(e, future));
                })
                .switchIfEmpty(Mono.fromRunnable(() -> future.complete(null)))
                .subscribe();
    }

    private void handleProjectionError(Throwable error, CompletableFuture<?> future) {
        if (error instanceof EventProjectionClient.ProjectionClientException pce &&
                pce.getErrorType() == EventProjectionClient.ProjectionClientException.ErrorType.NOT_FOUND) {

            // This is a 404 Not Found error. Treat it as a success.
            log.warn("Resource not found during projection for topic '{}', which is expected during a rebuild. Acknowledging and skipping message. Details: {}",
                    "events", error.getMessage());
            future.complete(null); // Complete the future successfully
        } else {
            // For all other errors, complete exceptionally to trigger a retry.
            log.error("A non-recoverable error occurred during projection for topic '{}'. The message will be retried.",
                    "events", error);
            future.completeExceptionally(error);
        }
    }
}
