package com.logmonitor.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request to fetch logs using a whitelisted command key.
 */
@Schema(description = "Log fetch request")
public class LogFetchRequest {

    @NotNull(message = "Server ID is required")
    @Schema(description = "Target server ID", example = "1")
    private Long serverId;

    @NotBlank(message = "Command key is required")
    @Schema(description = "Whitelisted command key", example = "TOMCAT_LOG")
    private String commandKey;

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
}
