package com.ticketly.mseventseatingprojection.controller;

import com.ticketly.mseventseatingprojection.dto.internal.PreOrderValidationResponse;
import com.ticketly.mseventseatingprojection.dto.internal.SeatDetailsResponse;
import com.ticketly.mseventseatingprojection.dto.internal.SeatInfoRequest;
import com.ticketly.mseventseatingprojection.dto.internal.SeatValidationResponse;
import com.ticketly.mseventseatingprojection.service.EventQueryService;
import com.ticketly.mseventseatingprojection.service.SeatService;
import dto.CreateOrderRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InternalQueryControllerTest {

    @Mock
    private SeatService seatService;

    @Mock
    private EventQueryService eventQueryService;

    @InjectMocks
    private InternalQueryController internalQueryController;

    @Test
    void validateSeats_whenAllSeatsAvailable_shouldReturnOk() {
        // Arrange
        String sessionId = "test-session-id";
        SeatInfoRequest request = createSeatInfoRequest();
        
        SeatValidationResponse validationResponse = new SeatValidationResponse(true, null);
        when(seatService.validateSeatsAvailability(eq(sessionId), any(SeatInfoRequest.class)))
                .thenReturn(Mono.just(validationResponse));

        // Act & Assert
        StepVerifier.create(internalQueryController.validateSeats(sessionId, request))
                .assertNext(response -> {
                    assert response.getStatusCode() == HttpStatus.OK;
                    assert response.getBody() != null;
                    assert response.getBody().isAllAvailable();
                })
                .verifyComplete();
    }

    @Test
    void validateSeats_whenSeatsUnavailable_shouldReturnConflict() {
        // Arrange
        String sessionId = "test-session-id";
        SeatInfoRequest request = createSeatInfoRequest();
        
        List<String> unavailableSeats = Arrays.asList("seat-1", "seat-2");
        SeatValidationResponse validationResponse = new SeatValidationResponse(false, unavailableSeats);
        when(seatService.validateSeatsAvailability(eq(sessionId), any(SeatInfoRequest.class)))
                .thenReturn(Mono.just(validationResponse));

        // Act & Assert
        StepVerifier.create(internalQueryController.validateSeats(sessionId, request))
                .assertNext(response -> {
                    assert response.getStatusCode() == HttpStatus.CONFLICT;
                    assert response.getBody() != null;
                    assert !response.getBody().isAllAvailable();
                    assert response.getBody().getUnavailableSeats().size() == 2;
                })
                .verifyComplete();
    }

    @Test
    void getSeatDetails_shouldReturnFluxOfSeatDetails() {
        // Arrange
        String sessionId = "test-session-id";
        SeatInfoRequest request = createSeatInfoRequest();
        
        SeatDetailsResponse seatDetails1 = createSeatDetails("seat-1", "A1");
        SeatDetailsResponse seatDetails2 = createSeatDetails("seat-2", "A2");
        
        // Setup mock with the exact same SeatInfoRequest instance
        when(seatService.getSeatDetails(eq(sessionId), same(request)))
                .thenReturn(Flux.just(seatDetails1, seatDetails2));

        // Act & Assert
        StepVerifier.create(internalQueryController.getSeatDetails(sessionId, request))
                .expectNext(seatDetails1)
                .expectNext(seatDetails2)
                .verifyComplete();
    }

    @Test
    void validatePreOrder_shouldReturnResponseEntity() {
        // Arrange
        CreateOrderRequest request = CreateOrderRequest.builder().build(); // Simplified for test
        PreOrderValidationResponse validationResponse = PreOrderValidationResponse.builder().build(); // Using builder
        
        // Make sure we're mocking the EventQueryService, not the controller itself
        when(eventQueryService.validatePreOrderDetails(any(CreateOrderRequest.class)))
                .thenReturn(Mono.just(validationResponse));

        // Act & Assert
        StepVerifier.create(internalQueryController.validatePreOrder(request))
                .assertNext(response -> {
                    assert response.getStatusCode() == HttpStatus.OK;
                    assert response.getBody() != null;
                    assert response.getBody() == validationResponse; // Make sure it's the same object
                })
                .verifyComplete();
    }

    private SeatInfoRequest createSeatInfoRequest() {
        SeatInfoRequest request = new SeatInfoRequest();
        request.setSeat_ids(Arrays.asList("seat-1", "seat-2", "seat-3"));
        request.setEvent_id(UUID.randomUUID());
        return request;
    }
    
    private SeatDetailsResponse createSeatDetails(String seatId, String label) {
        // Create a real UUID instead of trying to parse a string
        UUID validUuid = UUID.randomUUID();
        
        SeatDetailsResponse.TierInfo tierInfo = SeatDetailsResponse.TierInfo.builder()
                .id(UUID.randomUUID())
                .name("VIP")
                .price(new java.math.BigDecimal("100.00"))
                .color("#FF0000")
                .build();
                
        return SeatDetailsResponse.builder()
                .seatId(validUuid) // Use a valid UUID here
                .label(label)
                .tier(tierInfo)
                .build();
    }
}