package com.ticketly.mseventseatingprojection.service.mapper;

import dto.SessionSeatingMapDTO;
import model.EventDocument;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.stream.Collectors;

@Component
public class SessionSeatingMapper {

    public EventDocument.SessionSeatingMapInfo fromSessionMap(SessionSeatingMapDTO dto, Map<String, EventDocument.TierInfo> tierInfoMap) {
        if (dto == null) return null;
        return EventDocument.SessionSeatingMapInfo.builder()
                .name(dto.getName())
                .layout(fromLayout(dto.getLayout(), tierInfoMap))
                .build();
    }

    private EventDocument.LayoutInfo fromLayout(SessionSeatingMapDTO.Layout dto, Map<String, EventDocument.TierInfo> tierInfoMap) {
        if (dto == null) return null;
        return EventDocument.LayoutInfo.builder()
                .blocks(mapList(dto.getBlocks(), b -> fromBlock(b, tierInfoMap)))
                .build();
    }

    private EventDocument.BlockInfo fromBlock(SessionSeatingMapDTO.Block dto, Map<String, EventDocument.TierInfo> tierInfoMap) {
        if (dto == null) return null;
        return EventDocument.BlockInfo.builder()
                .id(dto.getId())
                .name(dto.getName())
                .type(dto.getType())
                .position(fromPosition(dto.getPosition()))
                .rows(mapList(dto.getRows(), r -> fromRow(r, tierInfoMap)))
                .seats(mapList(dto.getSeats(), s -> fromSeat(s, tierInfoMap)))
                .capacity(dto.getCapacity())
                .width(dto.getWidth())
                .height(dto.getHeight())
                .build();
    }

    private EventDocument.RowInfo fromRow(SessionSeatingMapDTO.Row dto, Map<String, EventDocument.TierInfo> tierInfoMap) {
        if (dto == null) return null;
        return EventDocument.RowInfo.builder()
                .id(dto.getId())
                .label(dto.getLabel())
                .seats(mapList(dto.getSeats(), s -> fromSeat(s, tierInfoMap)))
                .build();
    }

    private EventDocument.SeatInfo fromSeat(SessionSeatingMapDTO.Seat dto, Map<String, EventDocument.TierInfo> tierInfoMap) {
        if (dto == null) return null;
        return EventDocument.SeatInfo.builder()
                .id(dto.getId())
                .label(dto.getLabel())
                .status(dto.getStatus())
                .tier(dto.getTierId() != null ? tierInfoMap.get(dto.getTierId()) : null)
                .build();
    }

    private EventDocument.PositionInfo fromPosition(SessionSeatingMapDTO.Position dto) {
        if (dto == null) return null;
        return EventDocument.PositionInfo.builder()
                .x(dto.getX())
                .y(dto.getY())
                .build();
    }

    private <T, R> java.util.List<R> mapList(java.util.List<T> list, java.util.function.Function<T, R> mapper) {
        return list == null ? null : list.stream().map(mapper).collect(Collectors.toList());
    }
}
