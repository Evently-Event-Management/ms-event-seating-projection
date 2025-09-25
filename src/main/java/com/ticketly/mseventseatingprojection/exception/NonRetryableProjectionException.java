package com.ticketly.mseventseatingprojection.exception;

public class NonRetryableProjectionException extends RuntimeException {
    public NonRetryableProjectionException(String message, Throwable cause) {
        super(message, cause);
    }
}