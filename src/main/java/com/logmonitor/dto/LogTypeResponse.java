package com.logmonitor.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Describes an available log type from the command whitelist.
 */
@Schema(description = "Available log type")
public class LogTypeResponse {

    @Schema(description = "Display name", example = "Tomcat Catalina Log")
    private String logName;

    @Schema(description = "Command lookup key", example = "TOMCAT_LOG")
    private String commandKey;

    public LogTypeResponse() {
    }

    public LogTypeResponse(String logName, String commandKey) {
        this.logName = logName;
        this.commandKey = commandKey;
    }

    public String getLogName() {
        return logName;
    }

    public void setLogName(String logName) {
        this.logName = logName;
    }

    public String getCommandKey() {
        return commandKey;
    }

    public void setCommandKey(String commandKey) {
        this.commandKey = commandKey;
    }
}
