package com.ticketly.mseventseatingprojection.exception;

/**
 * Exception thrown when a user attempts to access a resource they are not authorized to access,
 * such as when a non-owner tries to access analytics data for an event they don't own.
 */
public class UnauthorizedAccessException extends RuntimeException {
    public UnauthorizedAccessException(String message) {
        super(message);
    }

    public UnauthorizedAccessException(String resourceType, String resourceId, String userId) {
        super(String.format("User '%s' is not authorized to access %s with ID: '%s'", userId, resourceType, resourceId));
    }
}
