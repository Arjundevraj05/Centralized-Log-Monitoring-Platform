package com.logmonitor.exception;

import java.time.Instant;

/**
 * Standard JSON error response body returned by the global exception handler.
 */
public class ApiErrorResponse {

    private Instant timestamp;
    private int status;
    private String message;
    private String path;

    public ApiErrorResponse() {
    }

    public ApiErrorResponse(Instant timestamp, int status, String message, String path) {
        this.timestamp = timestamp;
        this.status = status;
        this.message = message;
        this.path = path;
    }

    /**
     * Factory method for constructing an error response.
     *
     * @param timestamp error timestamp
     * @param status    HTTP status code
     * @param message   human-readable message
     * @param path      request path
     * @return populated error response
     */
    public static ApiErrorResponse of(Instant timestamp, int status, String message, String path) {
        return new ApiErrorResponse(timestamp, status, message, path);
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
