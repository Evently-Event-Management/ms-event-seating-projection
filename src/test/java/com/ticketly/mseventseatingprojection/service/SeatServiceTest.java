package com.ticketly.mseventseatingprojection.service;

import com.ticketly.mseventseatingprojection.dto.internal.SeatDetailsResponse;
import com.ticketly.mseventseatingprojection.dto.internal.SeatInfoRequest;
import com.ticketly.mseventseatingprojection.dto.internal.SeatValidationResponse;
import com.ticketly.mseventseatingprojection.model.EventDocument;
import com.ticketly.mseventseatingprojection.model.ReadModelSeatStatus;
import com.ticketly.mseventseatingprojection.repository.SeatRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SeatServiceTest {

    @Mock
    private SeatRepository seatRepository;

    @InjectMocks
    private SeatService seatService;

    @Test
    void validateSeatsAvailability_shouldReturnValidationResponse() {
        // Arrange
        String sessionId = "test-session-id";
        UUID eventId = UUID.randomUUID();
        List<String> seatIds = Arrays.asList("seat-1", "seat-2", "seat-3");
        
        SeatInfoRequest request = new SeatInfoRequest();
        request.setEvent_id(eventId);
        request.setSeat_ids(seatIds);
        
        SeatValidationResponse expectedResponse = new SeatValidationResponse(true, null);
        
        when(seatRepository.validateSeatsAvailability(
                eq(eventId.toString()), 
                eq(sessionId), 
                eq(seatIds)))
                .thenReturn(Mono.just(expectedResponse));

        // Act & Assert
        StepVerifier.create(seatService.validateSeatsAvailability(sessionId, request))
                .expectNext(expectedResponse)
                .verifyComplete();
    }

    @Test
    void getSeatDetails_shouldReturnSeatDetails() {
        // Arrange
        String sessionId = "test-session-id";
        UUID eventId = UUID.randomUUID();
        List<String> seatIds = Arrays.asList("seat-1", "seat-2");
        
        SeatInfoRequest request = new SeatInfoRequest();
        request.setEvent_id(eventId);
        request.setSeat_ids(seatIds);
        
        EventDocument.SeatInfo seatInfo = createSeatInfo("seat-1", "A1");
        
        // Mock the repository call
        when(seatRepository.findSeatDetails(
                eq(eventId.toString()), 
                eq(sessionId), 
                eq(seatIds)))
                .thenReturn(Flux.just(seatInfo));

        // No need to create expectedResponse, we'll use expectNextMatches instead

        // Act & Assert
        StepVerifier.create(seatService.getSeatDetails(sessionId, request))
                .expectNextMatches(response -> 
                    // We're using a random UUID now, so just check the label and tier name
                    response.getLabel().equals("A1") && 
                    response.getTier().getName().equals("VIP"))
                .verifyComplete();
    }
    
    private EventDocument.SeatInfo createSeatInfo(String id, String label) {
        // Create a valid UUID string for the seat ID
        String validUuidString = UUID.randomUUID().toString();
        
        EventDocument.TierInfo tierInfo = EventDocument.TierInfo.builder()
            .id(UUID.randomUUID().toString())
            .name("VIP")
            .price(new BigDecimal("100.00"))
            .color("#FF0000")
            .build();
            
        return EventDocument.SeatInfo.builder()
            .id(validUuidString) // Use a valid UUID string here
            .label(label)
            .tier(tierInfo)
            .build();
    }
    
    @Test
    void updateSeatStatus_withValidSeats_shouldUpdateSuccessfully() {
        // Arrange
        UUID sessionId = UUID.randomUUID();
        List<UUID> seatIds = Arrays.asList(UUID.randomUUID(), UUID.randomUUID());
        ReadModelSeatStatus newStatus = ReadModelSeatStatus.LOCKED;
        
        List<String> seatIdStrings = seatIds.stream()
                .map(UUID::toString)
                .collect(Collectors.toList());
        
        when(seatRepository.areAnySeatsBooked(eq(sessionId.toString()), eq(seatIdStrings)))
                .thenReturn(Mono.just(false));
        
        when(seatRepository.updateSeatStatuses(eq(sessionId.toString()), eq(seatIdStrings), eq(newStatus)))
                .thenReturn(Mono.empty());
                
        // Act & Assert
        StepVerifier.create(seatService.updateSeatStatus(sessionId, seatIds, newStatus))
                .expectNext(true)
                .verifyComplete();
    }
    
    @Test
    void updateSeatStatus_withAlreadyBookedSeats_shouldNotUpdate() {
        // Arrange
        UUID sessionId = UUID.randomUUID();
        List<UUID> seatIds = Arrays.asList(UUID.randomUUID(), UUID.randomUUID());
        ReadModelSeatStatus newStatus = ReadModelSeatStatus.LOCKED;
        
        List<String> seatIdStrings = seatIds.stream()
                .map(UUID::toString)
                .collect(Collectors.toList());
        
        when(seatRepository.areAnySeatsBooked(eq(sessionId.toString()), eq(seatIdStrings)))
                .thenReturn(Mono.just(true)); // Seats are already booked
                
        // Act & Assert
        StepVerifier.create(seatService.updateSeatStatus(sessionId, seatIds, newStatus))
                .expectNext(false)
                .verifyComplete();
    }
    
    @Test
    void updateSeatStatus_withEmptyList_shouldReturnFalse() {
        // Arrange
        UUID sessionId = UUID.randomUUID();
        List<UUID> seatIds = List.of(); // Empty list
        ReadModelSeatStatus newStatus = ReadModelSeatStatus.LOCKED;
        
        // Act & Assert
        StepVerifier.create(seatService.updateSeatStatus(sessionId, seatIds, newStatus))
                .expectNext(false)
                .verifyComplete();
    }
    
    @Test
    void updateSeatStatus_withRepositoryError_shouldReturnFalse() {
        // Arrange
        UUID sessionId = UUID.randomUUID();
        List<UUID> seatIds = Arrays.asList(UUID.randomUUID(), UUID.randomUUID());
        ReadModelSeatStatus newStatus = ReadModelSeatStatus.LOCKED;
        
        List<String> seatIdStrings = seatIds.stream()
                .map(UUID::toString)
                .collect(Collectors.toList());
        
        when(seatRepository.areAnySeatsBooked(eq(sessionId.toString()), eq(seatIdStrings)))
                .thenReturn(Mono.just(false));
        
        when(seatRepository.updateSeatStatuses(eq(sessionId.toString()), eq(seatIdStrings), eq(newStatus)))
                .thenReturn(Mono.error(new RuntimeException("Database error")));
                
        // Act & Assert
        StepVerifier.create(seatService.updateSeatStatus(sessionId, seatIds, newStatus))
                .expectNext(false)
                .verifyComplete();
    }
}