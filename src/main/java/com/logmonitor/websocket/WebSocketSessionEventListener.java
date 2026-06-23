package com.logmonitor.websocket;

import com.logmonitor.service.LogStreamingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

/**
 * Cleans up SSH log streams when a WebSocket client disconnects.
 */
@Component
public class WebSocketSessionEventListener {

    private static final Logger log = LoggerFactory.getLogger(WebSocketSessionEventListener.class);

    private final LogStreamingService logStreamingService;

    public WebSocketSessionEventListener(LogStreamingService logStreamingService) {
        this.logStreamingService = logStreamingService;
    }

    /**
     * Stops all active streams for a disconnected STOMP session.
     *
     * @param event session disconnect event
     */
    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        log.debug("WebSocket session disconnected: {}", sessionId);
        logStreamingService.stopStreamsForStompSession(sessionId);
    }
}
