package com.ticketly.mseventseatingprojection.controller;

import com.ticketly.mseventseatingprojection.dto.SessionCountDTO;
import com.ticketly.mseventseatingprojection.dto.SessionInfoDTO;
import com.ticketly.mseventseatingprojection.dto.read.DiscountDetailsDTO;
import com.ticketly.mseventseatingprojection.dto.read.EventBasicInfoDTO;
import com.ticketly.mseventseatingprojection.dto.read.EventThumbnailDTO;
import com.ticketly.mseventseatingprojection.service.EventQueryService;
import com.ticketly.mseventseatingprojection.service.EventTrendingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventQueryControllerTest {

    @Mock
    private EventQueryService eventQueryService;

    @Mock
    private EventTrendingService eventTrendingService;

    @InjectMocks
    private EventQueryController eventQueryController;

    @Test
    void searchEvents_shouldReturnPageOfEvents() {
        // Arrange
        String searchTerm = "concert";
        Page<EventThumbnailDTO> expectedPage = new PageImpl<>(
            Arrays.asList(
                EventThumbnailDTO.builder().id("1").title("Event 1").build(),
                EventThumbnailDTO.builder().id("2").title("Event 2").build()
            )
        );
        
        when(eventQueryService.searchEvents(
                eq(searchTerm), any(), any(), any(), any(),
                any(), any(), any(), any(), any()))
                .thenReturn(Mono.just(expectedPage));

        // Act & Assert
        StepVerifier.create(eventQueryController.searchEvents(
                searchTerm, null, null, null, null, 
                null, null, null, null, null))
                .assertNext(responseEntity -> {
                    assert responseEntity.getStatusCode() == HttpStatus.OK;
                    assert responseEntity.getBody() != null;
                    assert responseEntity.getBody().getTotalElements() == 2;
                })
                .verifyComplete();
    }

    @Test
    void getBasicEventInfo_shouldReturnEventInfo() {
        // Arrange
        String eventId = "event-1";
        EventBasicInfoDTO eventInfo = EventBasicInfoDTO.builder()
                .id(eventId)
                .title("Test Event")
                .description("Description")
                .build();
        
        when(eventQueryService.getBasicEventInfo(eventId))
                .thenReturn(Mono.just(eventInfo));

        // Act & Assert
        StepVerifier.create(eventQueryController.getBasicEventInfo(eventId))
                .assertNext(responseEntity -> {
                    assert responseEntity.getStatusCode() == HttpStatus.OK;
                    assert responseEntity.getBody() != null;
                    assert responseEntity.getBody().getId().equals(eventId);
                    assert responseEntity.getBody().getTitle().equals("Test Event");
                })
                .verifyComplete();
    }

    @Test
    void getEventSessions_shouldReturnSessionInfo() {
        // Arrange
        String eventId = "event-1";
        Page<SessionInfoDTO> sessionsPage = new PageImpl<>(Collections.emptyList());
        
        // Make sure we're mocking with a specific Pageable argument matcher
        when(eventQueryService.findSessionsBasicInfoByEventId(eq(eventId), any(Pageable.class)))
                .thenReturn(Mono.just(sessionsPage));

        // Act & Assert
        StepVerifier.create(eventQueryController.getEventSessions(eventId, PageRequest.of(0, 10)))
                .assertNext(responseEntity -> {
                    assert responseEntity.getStatusCode() == HttpStatus.OK;
                    assert responseEntity.getBody() != null;
                    assert responseEntity.getBody() == sessionsPage; // Verify it's the same object
                })
                .verifyComplete();
    }
    
    @Test
    void getSessionsInRange_shouldReturnSessions() {
        // Arrange
        String eventId = "event-1";
        Instant fromDate = Instant.now();
        Instant toDate = fromDate.plusSeconds(86400); // 1 day later
        List<SessionInfoDTO> sessions = Collections.emptyList();
        
        when(eventQueryService.findSessionsInRange(eventId, fromDate, toDate))
                .thenReturn(Flux.fromIterable(sessions));

        // Act & Assert
        StepVerifier.create(eventQueryController.getSessionsInRange(eventId, fromDate, toDate))
                .assertNext(responseEntity -> {
                    assert responseEntity.getStatusCode() == HttpStatus.OK;
                    assert responseEntity.getBody() != null;
                    assert responseEntity.getBody().isEmpty();
                })
                .verifyComplete();
    }
    
    @Test
    void getPublicDiscounts_shouldReturnDiscounts() {
        // Arrange
        String eventId = "event-1";
        String sessionId = "session-1";
        List<DiscountDetailsDTO> discounts = Collections.emptyList();
        
        when(eventQueryService.getPublicDiscountsForSession(eventId, sessionId))
                .thenReturn(Flux.fromIterable(discounts));

        // Act & Assert
        StepVerifier.create(eventQueryController.getPublicDiscounts(eventId, sessionId))
                .assertNext(responseEntity -> {
                    assert responseEntity.getStatusCode() == HttpStatus.OK;
                    assert responseEntity.getBody() != null;
                    assert responseEntity.getBody().isEmpty();
                })
                .verifyComplete();
    }
    
    @Test
    void getTopTrendingEvents_shouldReturnEvents() {
        // Arrange
        int limit = 5;
        EventThumbnailDTO event1 = EventThumbnailDTO.builder().id("1").title("Event 1").build();
        EventThumbnailDTO event2 = EventThumbnailDTO.builder().id("2").title("Event 2").build();
        
        when(eventTrendingService.getTopTrendingEventThumbnails(limit))
                .thenReturn(Flux.just(event1, event2));

        // Act & Assert
        StepVerifier.create(eventQueryController.getTopTrendingEvents(limit))
                .expectNext(event1)
                .expectNext(event2)
                .verifyComplete();
    }
    
    @Test
    void getTotalSessionsCount_shouldReturnCount() {
        // Arrange
        SessionCountDTO countDTO = new SessionCountDTO(100);
        
        when(eventQueryService.countAllSessions())
                .thenReturn(Mono.just(countDTO));

        // Act & Assert
        StepVerifier.create(eventQueryController.getTotalSessionsCount())
                .assertNext(responseEntity -> {
                    assert responseEntity.getStatusCode() == HttpStatus.OK;
                    assert responseEntity.getBody() != null;
                    assert responseEntity.getBody().getTotalSessions() == 100;
                })
                .verifyComplete();
    }
}