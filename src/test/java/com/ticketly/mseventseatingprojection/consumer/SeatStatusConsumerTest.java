package com.ticketly.mseventseatingprojection.consumer;

import com.ticketly.mseventseatingprojection.dto.SeatStatusChangeEventDto;
import com.ticketly.mseventseatingprojection.dto.read.SeatStatusUpdateDto;
import com.ticketly.mseventseatingprojection.model.ReadModelSeatStatus;
import com.ticketly.mseventseatingprojection.service.SeatService;
import com.ticketly.mseventseatingprojection.service.SseService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SeatStatusConsumerTest {

    @Mock
    private SseService sseService;

    @Mock
    private SeatService seatService;

    @Mock
    private Acknowledgment acknowledgment;

    @InjectMocks
    private SeatStatusConsumer seatStatusConsumer;

    @Captor
    private ArgumentCaptor<SeatStatusUpdateDto> updateDtoCaptor;

    @Captor
    private ArgumentCaptor<UUID> sessionIdCaptor;

    @Test
    void onSeatStatusChange_withLockedStatus_shouldUpdateStatusAndPublishSse() {
        // Arrange
        UUID sessionId = UUID.randomUUID();
        List<UUID> seatIds = Arrays.asList(UUID.randomUUID(), UUID.randomUUID());
        
        SeatStatusChangeEventDto payload = new SeatStatusChangeEventDto(
                sessionId,
                seatIds,
                ReadModelSeatStatus.LOCKED
        );
        
        when(seatService.updateSeatStatus(eq(sessionId), eq(seatIds), eq(ReadModelSeatStatus.LOCKED)))
                .thenReturn(Mono.just(true));
                
        doNothing().when(sseService).publish(any(SeatStatusUpdateDto.class), eq(sessionId));

        // Act
        seatStatusConsumer.onSeatStatusChange(payload, acknowledgment);

        // Assert
        verify(seatService).updateSeatStatus(eq(sessionId), eq(seatIds), eq(ReadModelSeatStatus.LOCKED));
        verify(sseService).publish(updateDtoCaptor.capture(), sessionIdCaptor.capture());
        verify(acknowledgment).acknowledge();
        
        assertEquals(ReadModelSeatStatus.LOCKED, updateDtoCaptor.getValue().status());
        assertEquals(seatIds, updateDtoCaptor.getValue().seatIds());
        assertEquals(sessionId, sessionIdCaptor.getValue());
    }

    @Test
    void onSeatStatusChange_withAvailableStatus_shouldUpdateStatusAndPublishSse() {
        // Arrange
        UUID sessionId = UUID.randomUUID();
        List<UUID> seatIds = Arrays.asList(UUID.randomUUID(), UUID.randomUUID());
        
        SeatStatusChangeEventDto payload = new SeatStatusChangeEventDto(
                sessionId,
                seatIds,
                ReadModelSeatStatus.AVAILABLE
        );
        
        when(seatService.updateSeatStatus(eq(sessionId), eq(seatIds), eq(ReadModelSeatStatus.AVAILABLE)))
                .thenReturn(Mono.just(true));
                
        doNothing().when(sseService).publish(any(SeatStatusUpdateDto.class), eq(sessionId));

        // Act
        seatStatusConsumer.onSeatStatusChange(payload, acknowledgment);

        // Assert
        verify(seatService).updateSeatStatus(eq(sessionId), eq(seatIds), eq(ReadModelSeatStatus.AVAILABLE));
        verify(sseService).publish(updateDtoCaptor.capture(), sessionIdCaptor.capture());
        verify(acknowledgment).acknowledge();
        
        assertEquals(ReadModelSeatStatus.AVAILABLE, updateDtoCaptor.getValue().status());
        assertEquals(seatIds, updateDtoCaptor.getValue().seatIds());
        assertEquals(sessionId, sessionIdCaptor.getValue());
    }

    @Test
    void onSeatStatusChange_withBookedStatus_shouldOnlyPublishSse() {
        // Arrange
        UUID sessionId = UUID.randomUUID();
        List<UUID> seatIds = Arrays.asList(UUID.randomUUID(), UUID.randomUUID());
        
        SeatStatusChangeEventDto payload = new SeatStatusChangeEventDto(
                sessionId,
                seatIds,
                ReadModelSeatStatus.BOOKED
        );
        
        doNothing().when(sseService).publish(any(SeatStatusUpdateDto.class), eq(sessionId));

        // Act
        seatStatusConsumer.onSeatStatusChange(payload, acknowledgment);

        // Assert
        // Should not call updateSeatStatus for BOOKED status
        verify(seatService, never()).updateSeatStatus(any(UUID.class), anyList(), any(ReadModelSeatStatus.class));
        verify(sseService).publish(updateDtoCaptor.capture(), sessionIdCaptor.capture());
        verify(acknowledgment).acknowledge();
        
        assertEquals(ReadModelSeatStatus.BOOKED, updateDtoCaptor.getValue().status());
        assertEquals(seatIds, updateDtoCaptor.getValue().seatIds());
        assertEquals(sessionId, sessionIdCaptor.getValue());
    }

    @Test
    void onSeatStatusChange_whenUpdateFails_shouldNotPublishSse() {
        // Arrange
        UUID sessionId = UUID.randomUUID();
        List<UUID> seatIds = Arrays.asList(UUID.randomUUID(), UUID.randomUUID());
        
        SeatStatusChangeEventDto payload = new SeatStatusChangeEventDto(
                sessionId,
                seatIds,
                ReadModelSeatStatus.LOCKED
        );
        
        when(seatService.updateSeatStatus(eq(sessionId), eq(seatIds), eq(ReadModelSeatStatus.LOCKED)))
                .thenReturn(Mono.just(false));

        // Act
        seatStatusConsumer.onSeatStatusChange(payload, acknowledgment);

        // Assert
        verify(seatService).updateSeatStatus(eq(sessionId), eq(seatIds), eq(ReadModelSeatStatus.LOCKED));
        verify(sseService, never()).publish(any(), any());
        verify(acknowledgment).acknowledge();
    }
}