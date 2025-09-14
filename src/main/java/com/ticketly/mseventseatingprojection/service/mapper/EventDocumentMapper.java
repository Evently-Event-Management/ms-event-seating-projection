package com.ticketly.mseventseatingprojection.service.mapper;

import com.ticketly.mseventseatingprojection.model.EventDocument;
import dto.SessionSeatingMapDTO;
import dto.projection.EventProjectionDTO;
import dto.projection.SessionProjectionDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class EventDocumentMapper {
    private final EventProjectionMapper eventProjectionMapper;
    private final SessionSeatingMapper sessionSeatingMapper;

    /**
     * Maps an EventProjectionDTO to an EventDocument.
     *
     * @param dto the EventProjectionDTO to map
     * @return the mapped EventDocument
     */
    public EventDocument fromProjection(EventProjectionDTO dto) {
        return eventProjectionMapper.fromProjection(dto);
    }

    /**
     * Maps a SessionProjectionDTO to an EventDocument.SessionInfo.
     *
     * @param dto the SessionProjectionDTO to map
     * @return the mapped SessionInfo
     */
    public EventDocument.SessionInfo fromProjectionSession(SessionProjectionDTO dto) {
        return eventProjectionMapper.fromSession(dto);
    }

    /**
     * Maps a SessionSeatingMapDTO and tier info map to an EventDocument.SessionSeatingMapInfo.
     *
     * @param dto the SessionSeatingMapDTO to map
     * @param tierInfoMap the map of tier IDs to TierInfo
     * @return the mapped SessionSeatingMapInfo
     */
    public EventDocument.SessionSeatingMapInfo fromSessionMap(SessionSeatingMapDTO dto, Map<String, EventDocument.TierInfo> tierInfoMap) {
        return sessionSeatingMapper.fromSessionMap(dto, tierInfoMap);
    }
}
