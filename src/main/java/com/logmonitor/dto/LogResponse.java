package com.logmonitor.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Response containing fetched or searched log lines.
 */
@Schema(description = "Log content response")
public class LogResponse {

    @Schema(description = "Target server ID", example = "1")
    private Long serverId;

    @Schema(description = "Whitelisted command key used", example = "TOMCAT_LOG")
    private String commandKey;

    @Schema(description = "Log lines returned from the remote server")
    private List<String> lines;

    @Schema(description = "Total number of lines", example = "42")
    private int lineCount;

    public LogResponse() {
    }

    public LogResponse(Long serverId, String commandKey, List<String> lines) {
        this.serverId = serverId;
        this.commandKey = commandKey;
        this.lines = lines;
        this.lineCount = lines != null ? lines.size() : 0;
    }

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

    public List<String> getLines() {
        return lines;
    }

    public void setLines(List<String> lines) {
        this.lines = lines;
        this.lineCount = lines != null ? lines.size() : 0;
    }

    public int getLineCount() {
        return lineCount;
    }

    public void setLineCount(int lineCount) {
        this.lineCount = lineCount;
    }
}
