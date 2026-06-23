package com.logmonitor.websocket;

import com.logmonitor.dto.LogStreamRequest;
import com.logmonitor.dto.LogStreamStopRequest;
import com.logmonitor.service.LogStreamingService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

/**
 * STOMP controller for real-time log streaming.
 *
 * <p>Clients subscribe to {@code /topic/logs} and send commands to {@code /app/logs/stream/*}.</p>
 */
@Controller
public class LogWebSocketController {

    private static final Logger log = LoggerFactory.getLogger(LogWebSocketController.class);

    private final LogStreamingService logStreamingService;

    public LogWebSocketController(LogStreamingService logStreamingService) {
        this.logStreamingService = logStreamingService;
    }

    /**
     * Starts tailing logs from a remote server via SSH.
     *
     * @param request         stream parameters
     * @param headerAccessor  STOMP session metadata
     */
    @MessageMapping("/logs/stream/start")
    public void startStream(@Valid @Payload LogStreamRequest request,
                            SimpMessageHeaderAccessor headerAccessor) {
        String stompSessionId = headerAccessor.getSessionId();
        String streamId = request.getStreamId() != null ? request.getStreamId() : stompSessionId;

        log.debug("WebSocket stream start: stompSession={}, streamId={}, serverId={}, commandKey={}",
                stompSessionId, streamId, request.getServerId(), request.getCommandKey());

        logStreamingService.startStream(
                stompSessionId,
                streamId,
                request.getServerId(),
                request.getCommandKey()
        );
    }

    /**
     * Stops an active log stream.
     *
     * @param request stop request with stream ID
     */
    @MessageMapping("/logs/stream/stop")
    public void stopStream(@Valid @Payload LogStreamStopRequest request) {
        log.debug("WebSocket stream stop: streamId={}", request.getStreamId());
        logStreamingService.stopStream(request.getStreamId());
    }
}
