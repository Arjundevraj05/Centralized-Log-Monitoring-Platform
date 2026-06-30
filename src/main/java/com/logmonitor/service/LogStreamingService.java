package com.logmonitor.service;

import com.logmonitor.config.WebSocketConfig;
import com.logmonitor.dto.LogStreamMessage;
import com.logmonitor.exception.SshOperationException;
import com.logmonitor.ssh.SSHService;
import com.logmonitor.ssh.SshConnection;
import com.logmonitor.util.SecurityUtils;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manages thread-safe real-time log streaming sessions over SSH.
 *
 * <p>Each stream runs on a dedicated worker thread. SSH connections are closed on stop,
 * error, or WebSocket session disconnect.</p>
 */
@Service
public class LogStreamingService {

    private static final Logger log = LoggerFactory.getLogger(LogStreamingService.class);
    private static final AtomicInteger STREAM_THREAD_COUNTER = new AtomicInteger();

    private final LogService logService;
    private final AppLogService appLogService;
    private final SSHService sshService;
    private final SimpMessagingTemplate messagingTemplate;
    private final ExecutorService streamExecutor;
    private final ConcurrentHashMap<String, StreamSession> activeStreams = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Set<String>> stompSessionStreams = new ConcurrentHashMap<>();

    public LogStreamingService(LogService logService,
                               AppLogService appLogService,
                               SSHService sshService,
                               SimpMessagingTemplate messagingTemplate) {
        this.logService = logService;
        this.appLogService = appLogService;
        this.sshService = sshService;
        this.messagingTemplate = messagingTemplate;
        this.streamExecutor = Executors.newCachedThreadPool(r -> {
            Thread thread = new Thread(r);
            thread.setName("log-stream-" + STREAM_THREAD_COUNTER.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        });
    }

    /**
     * Starts streaming logs for the given server using a whitelisted tail command.
     *
     * @param stompSessionId WebSocket STOMP session ID
     * @param streamId       client stream identifier
     * @param serverId       target server ID
     * @param commandKey     whitelisted streaming command key (e.g. TOMCAT_TAIL)
     */
    public void startStream(String stompSessionId, String streamId, Long serverId, String commandKey) {
        if (activeStreams.containsKey(streamId)) {
            stopStream(streamId);
        }

        String username = SecurityUtils.getCurrentUsernameOrDefault("SYSTEM");
        String command = logService.resolveStreamCommand(commandKey);

        StreamSession session = new StreamSession(stompSessionId, streamId, serverId, commandKey);
        activeStreams.put(streamId, session);
        stompSessionStreams
                .computeIfAbsent(stompSessionId, key -> ConcurrentHashMap.newKeySet())
                .add(streamId);

        Future<?> future = streamExecutor.submit(() -> runStream(session, command, username));
        session.setFuture(future);

        log.info("Started log stream: streamId={}, serverId={}, commandKey={}, user={}",
                streamId, serverId, commandKey, username);
    }

    /**
     * Starts live tail of an application log using a cached logback path.
     */
    public void startAppLogStream(String stompSessionId, String streamId, Long logConfigId) {
        if (activeStreams.containsKey(streamId)) {
            stopStream(streamId);
        }

        String username = SecurityUtils.getCurrentUsernameOrDefault("SYSTEM");
        String command = appLogService.resolveLiveStreamCommand(logConfigId);
        Long serverId = appLogService.resolveServerId(logConfigId);
        String commandKey = "APP_LOG_LIVE:" + logConfigId;

        StreamSession session = new StreamSession(stompSessionId, streamId, serverId, commandKey);
        activeStreams.put(streamId, session);
        stompSessionStreams
                .computeIfAbsent(stompSessionId, key -> ConcurrentHashMap.newKeySet())
                .add(streamId);

        Future<?> future = streamExecutor.submit(() -> runStream(session, command, username));
        session.setFuture(future);

        log.info("Started app log stream: streamId={}, logConfigId={}, user={}",
                streamId, logConfigId, username);
    }

    /**
     * Stops an active log stream and closes its SSH connection.
     *
     * @param streamId stream identifier
     */
    public void stopStream(String streamId) {
        StreamSession session = activeStreams.remove(streamId);
        if (session == null) {
            return;
        }
        session.cancel();
        removeStreamFromStompSession(session.stompSessionId(), streamId);
        log.info("Stopped log stream: streamId={}", streamId);
    }

    /**
     * Stops all streams associated with a disconnected WebSocket session.
     *
     * @param stompSessionId WebSocket STOMP session ID
     */
    public void stopStreamsForStompSession(String stompSessionId) {
        Set<String> streamIds = stompSessionStreams.remove(stompSessionId);
        if (streamIds == null || streamIds.isEmpty()) {
            return;
        }
        log.info("Stopping {} stream(s) for disconnected STOMP session {}", streamIds.size(), stompSessionId);
        streamIds.forEach(this::stopStream);
    }

    @PreDestroy
    void shutdown() {
        activeStreams.keySet().forEach(this::stopStream);
        streamExecutor.shutdownNow();
    }

    private void runStream(StreamSession session, String command, String username) {
        SshConnection connection = null;
        try {
            connection = logService.openConnection(session.serverId());
            session.setConnection(connection);

            sshService.streamLogs(connection, command, line -> {
                if (!session.isRunning()) {
                    return;
                }
                publishLogLine(session, line);
            });
        } catch (Exception ex) {
            log.warn("Log stream failed: streamId={}, user={}, error={}",
                    session.streamId(), username, ex.getMessage());
            String message = ex instanceof SshOperationException
                    ? ex.getMessage()
                    : "Log streaming failed";
            publishError(session, message);
        } finally {
            sshService.disconnect(connection);
            session.setConnection(null);
            activeStreams.remove(session.streamId());
            removeStreamFromStompSession(session.stompSessionId(), session.streamId());
            publishEnd(session);
        }
    }

    private void publishLogLine(StreamSession session, String line) {
        messagingTemplate.convertAndSend(
                WebSocketConfig.LOG_TOPIC,
                LogStreamMessage.logLine(session.streamId(), session.serverId(), session.commandKey(), line)
        );
    }

    private void publishError(StreamSession session, String error) {
        messagingTemplate.convertAndSend(
                WebSocketConfig.LOG_TOPIC,
                LogStreamMessage.error(session.streamId(), session.serverId(), session.commandKey(), error)
        );
    }

    private void publishEnd(StreamSession session) {
        messagingTemplate.convertAndSend(
                WebSocketConfig.LOG_TOPIC,
                LogStreamMessage.end(session.streamId(), session.serverId(), session.commandKey())
        );
    }

    private void removeStreamFromStompSession(String stompSessionId, String streamId) {
        Set<String> streams = stompSessionStreams.get(stompSessionId);
        if (streams != null) {
            streams.remove(streamId);
            if (streams.isEmpty()) {
                stompSessionStreams.remove(stompSessionId, streams);
            }
        }
    }

    private static final class StreamSession {

        private final String stompSessionId;
        private final String streamId;
        private final Long serverId;
        private final String commandKey;
        private final AtomicBoolean running = new AtomicBoolean(true);
        private volatile SshConnection connection;
        private volatile Future<?> future;

        private StreamSession(String stompSessionId, String streamId, Long serverId, String commandKey) {
            this.stompSessionId = stompSessionId;
            this.streamId = streamId;
            this.serverId = serverId;
            this.commandKey = commandKey;
        }

        private String stompSessionId() {
            return stompSessionId;
        }

        private String streamId() {
            return streamId;
        }

        private Long serverId() {
            return serverId;
        }

        private String commandKey() {
            return commandKey;
        }

        private boolean isRunning() {
            return running.get();
        }

        private void setConnection(SshConnection connection) {
            this.connection = connection;
        }

        private void setFuture(Future<?> future) {
            this.future = future;
        }

        private void cancel() {
            running.set(false);
            if (future != null) {
                future.cancel(true);
            }
            SshConnection activeConnection = connection;
            if (activeConnection != null) {
                activeConnection.disconnect();
            }
        }
    }
}
