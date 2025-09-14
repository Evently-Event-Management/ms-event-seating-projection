package com.ticketly.mseventseatingprojection.controller;

import com.ticketly.mseventseatingprojection.dto.read.SeatStatusUpdateDto;
import com.ticketly.mseventseatingprojection.service.SseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.UUID;

@RestController
@RequestMapping("/v1/sse")
@RequiredArgsConstructor
public class SseController {

    private final SseService sseService;

    /**
     * Streams seat status updates for a session using Server-Sent Events (SSE).
     *
     * @param sessionId The UUID of the session.
     * @return Flux of ServerSentEvent containing SeatStatusUpdateDto.
     */
    @GetMapping(path = "/sessions/{sessionId}/seat-status", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<SeatStatusUpdateDto>> streamSeatStatus(@PathVariable UUID sessionId) {
        return sseService.register(sessionId);
    }
}