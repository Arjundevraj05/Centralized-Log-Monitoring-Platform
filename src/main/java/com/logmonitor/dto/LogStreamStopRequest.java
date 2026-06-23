package com.logmonitor.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Request to stop an active log stream.
 */
@Schema(description = "Stop log streaming session")
public class LogStreamStopRequest {

    @NotBlank(message = "Stream ID is required")
    @Schema(description = "Stream identifier to stop", example = "abc-123")
    private String streamId;

    public String getStreamId() {
        return streamId;
    }

    public void setStreamId(String streamId) {
        this.streamId = streamId;
    }
}
