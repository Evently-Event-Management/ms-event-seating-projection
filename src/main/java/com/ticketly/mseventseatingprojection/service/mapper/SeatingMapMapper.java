package com.ticketly.mseventseatingprojection.service.mapper;

import com.ticketly.mseventseatingprojection.model.EventDocument;
import dto.SessionSeatingMapDTO;
import dto.projection.SeatingMapProjectionDTO;
import dto.projection.TierInfo;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * A centralized mapper responsible for converting various Seating Map DTOs
 * into the EventDocument's internal seating map representation.
 */
@Component
public class SeatingMapMapper extends BaseMapper {

    // --- Entry point for the full EventProjectionDTO ---
    public EventDocument.SessionSeatingMapInfo fromProjection(SeatingMapProjectionDTO dto) {
        if (dto == null) return null;
        return EventDocument.SessionSeatingMapInfo.builder()
                .name(dto.getName())
                .layout(fromLayout(dto.getLayout()))
                .build();
    }

    // --- Entry point for the SessionSeatingMapDTO (Read-Side Join) ---
    public EventDocument.SessionSeatingMapInfo fromSessionMap(SessionSeatingMapDTO dto, Map<String, EventDocument.TierInfo> tierInfoMap) {
        if (dto == null) return null;
        return EventDocument.SessionSeatingMapInfo.builder()
                .name(dto.getName())
                .layout(fromLayout(dto.getLayout(), tierInfoMap))
                .build();
    }

    // --- Private Helper Methods for different DTOs ---

    private EventDocument.LayoutInfo fromLayout(SeatingMapProjectionDTO.LayoutInfo dto) {
        if (dto == null) return null;
        return EventDocument.LayoutInfo.builder()
                .blocks(mapList(dto.getBlocks(), this::fromBlock))
                .build();
    }

    private EventDocument.LayoutInfo fromLayout(SessionSeatingMapDTO.Layout dto, Map<String, EventDocument.TierInfo> tierInfoMap) {
        if (dto == null) return null;
        return EventDocument.LayoutInfo.builder()
                .blocks(mapList(dto.getBlocks(), b -> fromBlock(b, tierInfoMap)))
                .build();
    }

    private EventDocument.BlockInfo fromBlock(SeatingMapProjectionDTO.BlockInfo dto) {
        if (dto == null) return null;
        return EventDocument.BlockInfo.builder()
                .id(String.valueOf(dto.getId()))
                .name(dto.getName())
                .type(dto.getType())
                .position(fromPosition(dto.getPosition()))
                .rows(mapList(dto.getRows(), this::fromRow))
                .seats(mapList(dto.getSeats(), this::fromSeat))
                .capacity(dto.getCapacity())
                .width(dto.getWidth())
                .height(dto.getHeight())
                .build();
    }

    private EventDocument.BlockInfo fromBlock(SessionSeatingMapDTO.Block dto, Map<String, EventDocument.TierInfo> tierInfoMap) {
        if (dto == null) return null;
        return EventDocument.BlockInfo.builder()
                .id(String.valueOf(dto.getId()))
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

    private EventDocument.RowInfo fromRow(SeatingMapProjectionDTO.RowInfo dto) {
        if (dto == null) return null;
        return EventDocument.RowInfo.builder()
                .id(String.valueOf(dto.getId()))
                .label(dto.getLabel())
                .seats(mapList(dto.getSeats(), this::fromSeat))
                .build();
    }

    private EventDocument.RowInfo fromRow(SessionSeatingMapDTO.Row dto, Map<String, EventDocument.TierInfo> tierInfoMap) {
        if (dto == null) return null;
        return EventDocument.RowInfo.builder()
                .id(String.valueOf(dto.getId()))
                .label(dto.getLabel())
                .seats(mapList(dto.getSeats(), s -> fromSeat(s, tierInfoMap)))
                .build();
    }

    private EventDocument.SeatInfo fromSeat(SeatingMapProjectionDTO.SeatInfo dto) {
        if (dto == null) return null;
        return EventDocument.SeatInfo.builder()
                .id(String.valueOf(dto.getId()))
                .label(dto.getLabel())
                .status(toSeatStatus(dto.getStatus() != null ? dto.getStatus().toString() : null))
                .tier(fromTier(dto.getTier()))
                .build();
    }

    private EventDocument.SeatInfo fromSeat(SessionSeatingMapDTO.Seat dto, Map<String, EventDocument.TierInfo> tierInfoMap) {
        if (dto == null) return null;
        EventDocument.TierInfo embeddedTier = dto.getTierId() != null ? tierInfoMap.get(dto.getTierId().toString()) : null;
        return EventDocument.SeatInfo.builder()
                .id(String.valueOf(dto.getId()))
                .label(dto.getLabel())
                .status(toSeatStatus(dto.getStatus().toString()))
                .tier(embeddedTier)
                .build();
    }

    private EventDocument.PositionInfo fromPosition(SeatingMapProjectionDTO.PositionInfo dto) {
        if (dto == null) return null;
        return EventDocument.PositionInfo.builder().x(dto.getX()).y(dto.getY()).build();
    }

    private EventDocument.PositionInfo fromPosition(SessionSeatingMapDTO.Position dto) {
        if (dto == null) return null;
        return EventDocument.PositionInfo.builder().x(dto.getX()).y(dto.getY()).build();
    }

    private EventDocument.TierInfo fromTier(TierInfo dto) {
        if (dto == null) return null;
        return EventDocument.TierInfo.builder()
                .id(dto.getId().toString())
                .name(dto.getName())
                .price(dto.getPrice())
                .color(dto.getColor())
                .build();
    }
}