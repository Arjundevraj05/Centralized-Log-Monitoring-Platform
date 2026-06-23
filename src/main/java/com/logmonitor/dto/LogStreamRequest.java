package com.logmonitor.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request to start a real-time log stream over WebSocket.
 */
@Schema(description = "Start log streaming session")
public class LogStreamRequest {

    @NotNull(message = "Server ID is required")
    @Schema(description = "Target server ID", example = "1")
    private Long serverId;

    @NotBlank(message = "Command key is required")
    @Schema(description = "Whitelisted streaming command key", example = "TOMCAT_TAIL")
    private String commandKey;

    @Schema(description = "Optional client stream identifier; defaults to WebSocket session ID")
    private String streamId;

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

    public String getStreamId() {
        return streamId;
    }

    public void setStreamId(String streamId) {
        this.streamId = streamId;
    }
}
