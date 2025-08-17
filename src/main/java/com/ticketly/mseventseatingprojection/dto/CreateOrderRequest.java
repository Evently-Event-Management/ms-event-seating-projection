package com.ticketly.mseventseatingprojection.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CreateOrderRequest {
    @NotNull(message = "Event ID cannot be null")
    private String eventId;
    @NotNull(message = "Session ID cannot be null")
    private String sessionId;
    @NotEmpty
    private List<String> seatIds;
}
