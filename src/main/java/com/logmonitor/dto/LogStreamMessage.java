package com.logmonitor.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Message published to {@code /topic/logs} during real-time streaming.
 */
@Schema(description = "Real-time log stream message")
public class LogStreamMessage {

    public enum MessageType {
        LOG,
        ERROR,
        END
    }

    @Schema(description = "Stream session identifier")
    private String streamId;

    @Schema(description = "Target server ID", example = "1")
    private Long serverId;

    @Schema(description = "Whitelisted command key", example = "TOMCAT_TAIL")
    private String commandKey;

    @Schema(description = "Log line content")
    private String line;

    @Schema(description = "Message type", example = "LOG")
    private MessageType type;

    @Schema(description = "Message timestamp")
    private Instant timestamp;

    public LogStreamMessage() {
    }

    public static LogStreamMessage logLine(String streamId, Long serverId, String commandKey, String line) {
        LogStreamMessage message = new LogStreamMessage();
        message.streamId = streamId;
        message.serverId = serverId;
        message.commandKey = commandKey;
        message.line = line;
        message.type = MessageType.LOG;
        message.timestamp = Instant.now();
        return message;
    }

    public static LogStreamMessage error(String streamId, Long serverId, String commandKey, String error) {
        LogStreamMessage message = new LogStreamMessage();
        message.streamId = streamId;
        message.serverId = serverId;
        message.commandKey = commandKey;
        message.line = error;
        message.type = MessageType.ERROR;
        message.timestamp = Instant.now();
        return message;
    }

    public static LogStreamMessage end(String streamId, Long serverId, String commandKey) {
        LogStreamMessage message = new LogStreamMessage();
        message.streamId = streamId;
        message.serverId = serverId;
        message.commandKey = commandKey;
        message.type = MessageType.END;
        message.timestamp = Instant.now();
        return message;
    }

    public String getStreamId() {
        return streamId;
    }

    public void setStreamId(String streamId) {
        this.streamId = streamId;
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

    public String getLine() {
        return line;
    }

    public void setLine(String line) {
        this.line = line;
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
