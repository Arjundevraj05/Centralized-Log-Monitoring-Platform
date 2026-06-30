package com.logmonitor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class AppLogStreamRequest {

    @NotNull
    private Long logConfigId;

    @NotBlank
    private String streamId;

    public Long getLogConfigId() { return logConfigId; }
    public void setLogConfigId(Long logConfigId) { this.logConfigId = logConfigId; }
    public String getStreamId() { return streamId; }
    public void setStreamId(String streamId) { this.streamId = streamId; }
}
