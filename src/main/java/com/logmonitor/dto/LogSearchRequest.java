package com.logmonitor.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request to search logs using a whitelisted search command.
 */
@Schema(description = "Log search request")
public class LogSearchRequest {

    @NotNull(message = "Server ID is required")
    @Schema(description = "Target server ID", example = "1")
    private Long serverId;

    @NotBlank(message = "Command key is required")
    @Schema(description = "Base log command key", example = "TOMCAT_LOG")
    private String commandKey;

    @NotBlank(message = "Search term is required")
    @Size(min = 1, max = 100, message = "Search term must be between 1 and 100 characters")
    @Schema(description = "Term to search for (alphanumeric, spaces, dash, dot, underscore only)",
            example = "OutOfMemoryError")
    private String searchTerm;

    public Long getServerId() {
        return serverId;
    }

    public void setServerId(Long serverId) {
        this.serverId = serverId;
    }

    public String getCommandKey() {
        return commandKey;
    }

    public void setCommandKey(String commandKey) {
        this.commandKey = commandKey;
    }

    public String getSearchTerm() {
        return searchTerm;
    }

    public void setSearchTerm(String searchTerm) {
        this.searchTerm = searchTerm;
    }
}
