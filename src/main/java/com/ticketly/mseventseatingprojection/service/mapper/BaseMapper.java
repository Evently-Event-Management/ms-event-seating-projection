package com.ticketly.mseventseatingprojection.service.mapper;

import com.ticketly.mseventseatingprojection.model.ReadModelSeatStatus;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A base class for mappers providing common utility methods.
 */
public abstract class BaseMapper {

    /**
     * Safely maps a list of one type to a list of another type.
     *
     * @param list the source list
     * @param mapper the function to apply to each element
     * @return a new list with mapped elements, or an empty list if the source is null
     * @param <T> the source type
     * @param <R> the target type
     */
    protected <T, R> List<R> mapList(List<T> list, Function<T, R> mapper) {
        if (list == null) {
            return Collections.emptyList();
        }
        return list.stream().map(mapper).collect(Collectors.toList());
    }

    /**
     * Safely converts a status string to the ReadModelSeatStatus enum.
     *
     * @param statusString the status from the DTO
     * @return the corresponding enum value, or AVAILABLE as a default
     */
    protected ReadModelSeatStatus toSeatStatus(String statusString) {
        if (statusString == null) {
            return ReadModelSeatStatus.AVAILABLE;
        }
        try {
            return ReadModelSeatStatus.valueOf(statusString.toUpperCase());
        } catch (IllegalArgumentException e) {
            // Log a warning here if necessary
            return ReadModelSeatStatus.AVAILABLE;
        }
    }
}