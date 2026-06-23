package com.logmonitor.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Audit log entry returned by the API.
 */
@Schema(description = "Audit log entry")
public class AuditLogResponse {

    @Schema(description = "Audit entry ID", example = "1")
    private Long id;

    @Schema(description = "Username who performed the action", example = "admin")
    private String username;

    @Schema(description = "Action type", example = "USER_LOGIN")
    private String action;

    @Schema(description = "Affected resource", example = "admin")
    private String resource;

    @Schema(description = "When the action occurred")
    private Instant timestamp;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
