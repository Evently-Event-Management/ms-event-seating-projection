package com.ticketly.mseventseatingprojection.dto.internal;

import com.ticketly.mseventseatingprojection.dto.read.DiscountDetailsDTO;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PreOrderValidationResponse {
    private List<SeatDetailsResponse> seats;
    private DiscountDetailsDTO discount;
}
