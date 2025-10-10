package com.ticketly.mseventseatingprojection.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketly.mseventseatingprojection.dto.*;
import com.ticketly.mseventseatingprojection.exception.NonRetryableProjectionException;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class DebeziumEventConsumer {

    private final ProjectorService projectorService;
    private final EventRepository eventReadRepository;
    private final ObjectMapper objectMapper;
    private final EventReadRepositoryCustom eventReadRepositoryCustom;

    @KafkaListener(topics = {
            "dbz.ticketly.public.events",
            "dbz.ticketly.public.event_sessions",
            "dbz.ticketly.public.session_seating_maps",
            "dbz.ticketly.public.organizations",
            "dbz.ticketly.public.categories",
            "dbz.ticketly.public.event_cover_photos",
            "dbz.ticketly.public.discounts",
            "dbz.ticketly.public.discount_tiers",
            "dbz.ticketly.public.discount_sessions",
            "dbz.ticketly.public.tiers"
    }, containerFactory = "debeziumListenerContainerFactory")
    public void onDebeziumEvent(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        String topic = record.topic();
        String payload = record.value();

        if (payload == null || payload.isBlank()) {
            log.debug("Received tombstone record on topic {}. Ignoring.", topic);
            acknowledgment.acknowledge();
            return;
        }

        // 1. Build the appropriate reactive processing chain.
        Mono<Void> processingMono = switch (topic.substring(topic.lastIndexOf('.') + 1)) {
            case "events" -> processEventChange(payload);
            case "event_sessions" -> processSessionChange(payload);
            case "session_seating_maps" -> processSeatingMapChange(payload);
            case "organizations" -> processOrganizationChange(payload);
            case "categories" -> processCategoryChange(payload);
            case "event_cover_photos" -> processCoverPhotoChange(payload);
            case "discounts" -> processDiscountMetadataChange(payload);
            case "discount_tiers", "discount_sessions" -> processDiscountRelationshipChange(payload);
            case "tiers" -> processTierChange(payload);
            default -> {
                log.warn("Unhandled topic: {}", topic);
                yield Mono.empty();
            }
        };


        processingMono.block();

        log.info("Processing completed successfully for message on topic: {}", topic);
        acknowledgment.acknowledge();
    }

    private Mono<Void> processEventChange(String payload) {
        try {
            JsonNode message = objectMapper.readTree(payload).path("payload");
            String operation = message.path("op").asText();
            log.debug("processEventChange - operation: {}", operation);

            if ("d".equals(operation)) {
                UUID eventId = UUID.fromString(message.path("before").path("id").asText());
                log.info("Event delete detected for id: {}. Deleting projection.", eventId);
                return projectorService.deleteEvent(eventId)
                        .doOnSuccess(v -> log.debug("deleteEvent completed for id: {}", eventId))
                        .onErrorResume(this::handleProjectionError);
            }

            EventChangePayload eventChange = objectMapper.treeToValue(message.path("after"), EventChangePayload.class);
            log.debug("Event after payload id={} status={}", eventChange.getId(), eventChange.getStatus());

            if ("APPROVED".equals(eventChange.getStatus()) || "COMPLETED".equals(eventChange.getStatus())) {
                log.info("Projecting full event id={} status={}", eventChange.getId(), eventChange.getStatus());
                return projectorService.projectFullEvent(eventChange.getId())
                        .doOnSuccess(v -> log.debug("projectFullEvent completed for id: {}", eventChange.getId()))
                        .onErrorResume(this::handleProjectionError);
            } else {
                log.info("Removing projection for event id={} due to status={}", eventChange.getId(), eventChange.getStatus());
                return projectorService.deleteEvent(eventChange.getId())
                        .doOnSuccess(v -> log.debug("deleteEvent completed for id: {}", eventChange.getId()))
                        .onErrorResume(this::handleProjectionError);
            }
        } catch (JsonProcessingException e) {
            return Mono.error(new NonRetryableProjectionException("Failed to parse event change payload", e));
        }
    }

    private Mono<Void> processSessionChange(String payload) {
        try {
            JsonNode message = objectMapper.readTree(payload).path("payload");
            String operation = message.path("op").asText();
            log.debug("processSessionChange - operation: {}", operation);

            // For delete operation
            if ("d".equals(operation)) {
                SessionChangePayload sessionChange = objectMapper.treeToValue(message.path("before"), SessionChangePayload.class);
                log.debug("Session delete detected for eventId={} sessionId={}", sessionChange.getEventId(), sessionChange.getId());

                return eventReadRepository.existsById(sessionChange.getEventId().toString())
                    .flatMap(exists -> {
                        if (exists) {
                            log.info("Deleting session for eventId={} sessionId={}", sessionChange.getEventId(), sessionChange.getId());
                            return projectorService.deleteSession(sessionChange.getEventId(), sessionChange.getId())
                                .doOnSuccess(v -> log.debug("deleteSession completed for eventId={} sessionId={}",
                                        sessionChange.getEventId(), sessionChange.getId()))
                                .onErrorResume(this::handleProjectionError);
                        } else {
                            log.debug("Event not present in read model for eventId={}. Skipping session deletion.", sessionChange.getEventId());
                            return Mono.empty();
                        }
                    });
            }

            SessionChangePayload sessionChange = objectMapper.treeToValue(message.path("after"), SessionChangePayload.class);
            log.debug("SessionChange payload eventId={} sessionId={}", sessionChange.getEventId(), sessionChange.getId());

            return eventReadRepository.existsById(sessionChange.getEventId().toString())
                    .flatMap(exists -> {
                        if (exists) {
                            if ("c".equals(operation)) {
                                log.info("Creating new session for eventId={} sessionId={}", sessionChange.getEventId(), sessionChange.getId());
                                return projectorService.createSession(sessionChange.getEventId(), sessionChange.getId())
                                    .doOnSuccess(v -> log.debug("createSession completed for eventId={} sessionId={}",
                                            sessionChange.getEventId(), sessionChange.getId()))
                                    .onErrorResume(this::handleProjectionError);
                            } else {
                                log.info("Updating existing session for eventId={} sessionId={}", sessionChange.getEventId(), sessionChange.getId());
                                return projectorService.projectSessionUpdate(sessionChange.getEventId(), sessionChange.getId())
                                    .doOnSuccess(v -> log.debug("projectSessionUpdate completed for eventId={} sessionId={}",
                                            sessionChange.getEventId(), sessionChange.getId()))
                                    .onErrorResume(this::handleProjectionError);
                            }
                        } else {
                            log.debug("Event not present in read model for eventId={}. Skipping session projection.", sessionChange.getEventId());
                            return Mono.empty();
                        }
                    });
        } catch (JsonProcessingException e) {
            return Mono.error(new NonRetryableProjectionException("Failed to parse session change payload", e));
        }
    }

    private Mono<Void> processSeatingMapChange(String payload) {
        try {
            JsonNode message = objectMapper.readTree(payload).path("payload");
            String operation = message.path("op").asText();
            log.debug("processSeatingMapChange - operation: {}", operation);

            if ("c".equals(operation) || "d".equals(operation)) {
                log.debug("Ignoring seating map create/delete operation: {}", operation);
                return Mono.empty();
            }

            SeatingMapChangePayload mapChange = objectMapper.treeToValue(message.path("after"), SeatingMapChangePayload.class);
            String sessionIdStr = mapChange.getSessionId().toString();
            log.debug("SeatingMapChange sessionId={} lookup starting", sessionIdStr);

            return eventReadRepositoryCustom.findSessionStatusById(sessionIdStr)
                    .publishOn(Schedulers.boundedElastic())
                    .flatMap(sessionStatusInfo -> {
                        log.debug("Found session status info for sessionId={}: {}", sessionIdStr, sessionStatusInfo);
                        String eventId = sessionStatusInfo.getId();
                        SessionStatus status = sessionStatusInfo.getSessionStatus();
                        UUID sessionId = mapChange.getSessionId();

                        if (SessionStatus.ON_SALE.equals(status)) {
                            log.info("Applying seating map patch for eventId={} sessionId={}", eventId, sessionIdStr);
                            return projectorService.projectSeatingMapPatch(
                                    UUID.fromString(eventId),
                                    sessionId,
                                    mapChange.getLayoutData());
                        } else if (SessionStatus.SCHEDULED.equals(status)) {
                            log.info("Session scheduled; projecting session update for eventId={} sessionId={}", eventId, sessionIdStr);
                            return projectorService.projectSessionUpdate(
                                    UUID.fromString(eventId),
                                    sessionId);
                        } else {
                            log.debug("Session status '{}' not relevant for seating changes. Skipping.", status);
                            return Mono.empty();
                        }
                    })
                    .onErrorResume(this::handleProjectionError)
                    .then();
        } catch (JsonProcessingException e) {
            return Mono.error(new NonRetryableProjectionException("Failed to parse seating map change payload", e));
        }
    }

    private Mono<Void> processOrganizationChange(String payload) {
        try {
            JsonNode message = objectMapper.readTree(payload).path("payload");
            String operation = message.path("op").asText();
            log.debug("processOrganizationChange - operation: {}", operation);

            if ("d".equals(operation)) {
                String orgId = message.path("before").path("id").asText();
                log.info("Organization delete detected for id={}. Deleting projection.", orgId);
                return projectorService.deleteOrganization(orgId)
                        .doOnSuccess(v -> log.debug("deleteOrganization completed for id: {}", orgId))
                        .onErrorResume(this::handleProjectionError);
            }

            OrganizationChangePayload orgChange = objectMapper.treeToValue(message.path("after"), OrganizationChangePayload.class);
            log.info("Projecting organization change for id={}", orgChange.getId());
            return projectorService.projectOrganizationChange(orgChange)
                    .doOnSuccess(v -> log.debug("projectOrganizationChange completed for id: {}", orgChange.getId()))
                    .onErrorResume(this::handleProjectionError);
        } catch (JsonProcessingException e) {
            return Mono.error(new NonRetryableProjectionException("Failed to parse organization change payload", e));
        }
    }

    private Mono<Void> processCategoryChange(String payload) {
        try {
            JsonNode message = objectMapper.readTree(payload).path("payload");
            String operation = message.path("op").asText();
            log.debug("processCategoryChange - operation: {}", operation);

            if ("d".equals(operation)) {
                String catId = message.path("before").path("id").asText();
                log.info("Category delete detected for id={}. Deleting projection.", catId);
                return projectorService.deleteCategory(catId)
                        .doOnSuccess(v -> log.debug("deleteCategory completed for id: {}", catId))
                        .onErrorResume(this::handleProjectionError);
            }

            CategoryChangePayload catChange = objectMapper.treeToValue(message.path("after"), CategoryChangePayload.class);
            log.info("Projecting category change for id={}", catChange.getId());
            return projectorService.projectCategoryChange(catChange.getId())
                    .doOnSuccess(v -> log.debug("projectCategoryChange completed for id: {}", catChange.getId()))
                    .onErrorResume(this::handleProjectionError);
        } catch (JsonProcessingException e) {
            return Mono.error(new NonRetryableProjectionException("Failed to parse category change payload", e));
        }
    }

    private Mono<Void> processCoverPhotoChange(String payload) {
        try {
            JsonNode message = objectMapper.readTree(payload).path("payload");
            String operation = message.path("op").asText();

            if ("u".equals(operation)) {
                return Mono.empty(); // Ignore updates
            }

            JsonNode dataNode = "d".equals(operation) ? message.path("before") : message.path("after");
            if (dataNode.isMissingNode()) {
                return Mono.empty();
            }

            CoverPhotoChangePayload photoChange = objectMapper.treeToValue(dataNode, CoverPhotoChangePayload.class);

            return eventReadRepository.existsById(photoChange.getEventId().toString())
                    .flatMap(exists -> {
                        if (!exists) return Mono.empty();

                        if ("c".equals(operation)) {
                            log.info("Projecting cover photo addition for event ID: {}", photoChange.getEventId());
                            return projectorService.projectCoverPhotoAdded(photoChange.getEventId(), photoChange.getPhotoUrl());
                        } else if ("d".equals(operation)) {
                            log.info("Projecting cover photo removal for event ID: {}", photoChange.getEventId());
                            return projectorService.projectCoverPhotoRemoved(photoChange.getEventId(), photoChange.getPhotoUrl());
                        } else {
                            return Mono.empty();
                        }
                    })
                    .onErrorResume(this::handleProjectionError);
        } catch (JsonProcessingException e) {
            return Mono.error(new NonRetryableProjectionException("Failed to parse cover photo change payload", e));
        }
    }

    private Mono<Void> processDiscountMetadataChange(String payload) {
        try {
            JsonNode message = objectMapper.readTree(payload).path("payload");
            String operation = message.path("op").asText();

            if ("d".equals(operation)) {
                DiscountMetadataChangePayload discountChange = objectMapper.treeToValue(message.path("before"), DiscountMetadataChangePayload.class);
                return projectorService.projectDiscountDeletion(discountChange.getEventId(), discountChange.getId())
                        .onErrorResume(this::handleProjectionError);
            }

            DiscountMetadataChangePayload discountChange = objectMapper.treeToValue(message.path("after"), DiscountMetadataChangePayload.class);

            return eventReadRepository.existsById(discountChange.getEventId().toString())
                    .flatMap(exists -> {
                        if (!exists) return Mono.empty();

                        if ("c".equals(operation)) {
                            log.info("Projecting full discount on create: {}", discountChange.getId());
                            return projectorService.projectFullDiscount(discountChange.getEventId(), discountChange.getId());
                        } else {
                            log.info("Patching discount on update: {}", discountChange.getId());
                            return projectorService.patchDiscount(discountChange);
                        }
                    })
                    .onErrorResume(this::handleProjectionError);
        } catch (JsonProcessingException e) {
            return Mono.error(new NonRetryableProjectionException("Failed to parse discount metadata change payload", e));
        }
    }

    private Mono<Void> processDiscountRelationshipChange(String payload) {
        try {
            JsonNode message = objectMapper.readTree(payload).path("payload");
            JsonNode dataNode = message.path("op").asText().equals("d") ? message.path("before") : message.path("after");
            if (dataNode.isMissingNode()) {
                return Mono.empty();
            }

            DiscountJoinTablePayload joinChange = objectMapper.treeToValue(dataNode, DiscountJoinTablePayload.class);
            UUID discountId = joinChange.getDiscountId();

            return eventReadRepository.findEventIdByDiscountId(discountId.toString())
                    .flatMap(eventDoc -> {
                        if (eventDoc == null) {
                            log.debug("Parent event not found for discount {}. Skipping projection.", discountId);
                            return Mono.empty();
                        }

                        log.info("Projecting full discount {} due to relationship change.", discountId);
                        return projectorService.projectFullDiscount(UUID.fromString(eventDoc.getId()), discountId);
                    })
                    .onErrorResume(this::handleProjectionError)
                    .then();
        } catch (JsonProcessingException e) {
            return Mono.error(new NonRetryableProjectionException("Failed to parse discount relationship change payload", e));
        }
    }

    private Mono<Void> processTierChange(String payload) {
        try {
            JsonNode message = objectMapper.readTree(payload).path("payload");
            String operation = message.path("op").asText();
            log.debug("processTierChange - operation: {}", operation);

            if ("d".equals(operation)) {
                log.info("Tier deletion detected. Ignoring as tiers are only deleted when events are deleted.");
                return Mono.empty();
            }

            TierChangePayload tierChange = objectMapper.treeToValue(message.path("after"), TierChangePayload.class);
            log.info("Tier change detected for eventId={} tierId={}. Projecting full event.", tierChange.getEventId(), tierChange.getId());

            return eventReadRepository.existsById(tierChange.getEventId().toString())
                    .flatMap(exists -> {
                        if (exists) {
                            log.info("Projecting full event for eventId={} due to tier change", tierChange.getEventId());
                            return projectorService.projectFullEvent(tierChange.getEventId())
                                    .doOnSuccess(v -> log.debug("projectFullEvent completed for eventId={} after tier change", tierChange.getEventId()))
                                    .onErrorResume(this::handleProjectionError);
                        } else {
                            log.debug("Event not present in read model for eventId={}. Skipping tier projection.", tierChange.getEventId());
                            return Mono.empty();
                        }
                    });
        } catch (JsonProcessingException e) {
            return Mono.error(new NonRetryableProjectionException("Failed to parse tier change payload", e));
        }
    }

    private Mono<Void> handleProjectionError(Throwable error) {
        if (error instanceof EventProjectionClient.ProjectionClientException pce &&
                pce.getErrorType() == EventProjectionClient.ProjectionClientException.ErrorType.NOT_FOUND) {

            log.warn("Resource not found during projection. Swallowing error and completing: {}", error.getMessage());
            return Mono.empty();
        }
        return Mono.error(error);
    }
}