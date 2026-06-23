package com.logmonitor.exception;

/**
 * Thrown when a log search term contains disallowed characters.
 */
public class InvalidSearchTermException extends RuntimeException {

    public InvalidSearchTermException(String message) {
        super(message);
    }
}
