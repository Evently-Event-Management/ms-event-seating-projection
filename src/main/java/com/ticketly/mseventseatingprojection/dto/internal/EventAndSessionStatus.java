package com.ticketly.mseventseatingprojection.dto.internal;

import model.EventStatus;
import model.SessionStatus;

public record EventAndSessionStatus(EventStatus eventStatus, SessionStatus sessionStatus) {}
