package com.ticketly.mseventseatingprojection.service.mapper;

import com.ticketly.mseventseatingprojection.dto.SessionSeatingMapDTO;
import com.ticketly.mseventseatingprojection.dto.projection.EventProjectionDTO;
import com.ticketly.mseventseatingprojection.dto.projection.SessionProjectionDTO;
import com.ticketly.mseventseatingprojection.model.EventDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class EventDocumentMapper {
    private final EventProjectionMapper eventProjectionMapper;
    private final SessionSeatingMapper sessionSeatingMapper;

    public EventDocument fromProjection(EventProjectionDTO dto) {
        return eventProjectionMapper.fromProjection(dto);
    }

    public EventDocument.SessionInfo fromProjectionSession(SessionProjectionDTO dto) {
        return eventProjectionMapper.fromSession(dto);
    }

    public EventDocument.SessionSeatingMapInfo fromSessionMap(SessionSeatingMapDTO dto, Map<String, EventDocument.TierInfo> tierInfoMap) {
        return sessionSeatingMapper.fromSessionMap(dto, tierInfoMap);
    }
}
