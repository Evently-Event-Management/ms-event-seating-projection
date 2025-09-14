package com.ticketly.mseventseatingprojection.dto.read;

import lombok.Builder;
import lombok.Data;
import model.SessionStatus;

@Data
@Builder
public class SessionStatusInfo {
    private String id;
    private SessionStatus sessionStatus;
}
