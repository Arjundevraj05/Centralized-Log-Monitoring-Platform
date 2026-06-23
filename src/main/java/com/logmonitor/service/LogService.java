package com.logmonitor.service;

import com.logmonitor.audit.AuditService;
import com.logmonitor.dto.LogFetchRequest;
import com.logmonitor.dto.LogResponse;
import com.logmonitor.dto.LogSearchRequest;
import com.logmonitor.dto.LogTypeResponse;
import com.logmonitor.entity.LogConfig;
import com.logmonitor.entity.Server;
import com.logmonitor.exception.CommandNotWhitelistedException;
import com.logmonitor.exception.InvalidSearchTermException;
import com.logmonitor.exception.ResourceNotFoundException;
import com.logmonitor.exception.SshOperationException;
import com.logmonitor.repository.LogConfigRepository;
import com.logmonitor.ssh.SSHService;
import com.logmonitor.ssh.SshConnection;
import com.logmonitor.ssh.SshConnectionParams;
import com.logmonitor.util.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Service for fetching and searching logs via whitelisted SSH commands only.
 *
 * <p>User-supplied input is never executed as shell commands. Only {@code command_text}
 * values from {@code log_config} are sent to the remote server.</p>
 */
@Service
public class LogService {

    private static final Logger log = LoggerFactory.getLogger(LogService.class);

    private static final String SEARCH_PLACEHOLDER = "__SEARCH_TERM__";
    private static final String SEARCH_KEY_SUFFIX = "_SEARCH";
    private static final String TAIL_KEY_SUFFIX = "_TAIL";
    private static final Pattern SAFE_SEARCH_TERM = Pattern.compile("^[a-zA-Z0-9_\\-. ]{1,100}$");

    private static final Set<String> EXCLUDED_LOG_TYPE_SUFFIXES = Set.of(SEARCH_KEY_SUFFIX, TAIL_KEY_SUFFIX);

    private final LogConfigRepository logConfigRepository;
    private final ServerService serverService;
    private final SSHService sshService;
    private final AuditService auditService;

    public LogService(LogConfigRepository logConfigRepository,
                      ServerService serverService,
                      SSHService sshService,
                      AuditService auditService) {
        this.logConfigRepository = logConfigRepository;
        this.serverService = serverService;
        this.sshService = sshService;
        this.auditService = auditService;
    }

    /**
     * Returns fetchable log types from the whitelist (excludes search and streaming commands).
     *
     * @return available log types
     */
    @Transactional(readOnly = true)
    public List<LogTypeResponse> getLogTypes() {
        return logConfigRepository.findAllByOrderByLogNameAsc().stream()
                .filter(this::isFetchableLogType)
                .map(config -> new LogTypeResponse(config.getLogName(), config.getCommandKey()))
                .toList();
    }

    /**
     * Fetches logs from a remote server using a whitelisted command key.
     *
     * @param request fetch request with server ID and command key
     * @return log lines from the remote server
     */
    public LogResponse fetchLogs(LogFetchRequest request) {
        String username = SecurityUtils.getCurrentUsernameOrDefault("SYSTEM");
        Server server = getActiveServer(request.getServerId());
        String command = resolveFetchCommand(request.getCommandKey());

        log.info("Fetching logs: serverId={}, commandKey={}, user={}",
                request.getServerId(), request.getCommandKey(), username);

        String output = executeRemoteCommand(server, command);
        auditService.auditLogFetch(username, request.getServerId(), request.getCommandKey());

        List<String> lines = splitLines(output);
        return new LogResponse(request.getServerId(), request.getCommandKey(), lines);
    }

    /**
     * Searches logs on a remote server using a whitelisted search command.
     *
     * @param request search request with server ID, command key, and sanitized search term
     * @return matching log lines
     */
    public LogResponse searchLogs(LogSearchRequest request) {
        String username = SecurityUtils.getCurrentUsernameOrDefault("SYSTEM");
        Server server = getActiveServer(request.getServerId());
        String sanitizedTerm = sanitizeSearchTerm(request.getSearchTerm());
        String command = resolveSearchCommand(request.getCommandKey(), sanitizedTerm);

        log.info("Searching logs: serverId={}, commandKey={}, user={}",
                request.getServerId(), request.getCommandKey(), username);

        String output = executeRemoteCommand(server, command);
        auditService.auditLogSearch(username, request.getServerId(), request.getCommandKey());

        List<String> lines = splitLines(output);
        return new LogResponse(request.getServerId(), request.getCommandKey(), lines);
    }

    /**
     * Resolves a whitelisted fetch command by key.
     *
     * @param commandKey lookup key
     * @return approved command text
     */
    @Transactional(readOnly = true)
    public String resolveFetchCommand(String commandKey) {
        LogConfig config = logConfigRepository.findByCommandKey(commandKey)
                .orElseThrow(() -> new CommandNotWhitelistedException(commandKey));

        if (!isFetchableLogType(config)) {
            throw new CommandNotWhitelistedException(commandKey);
        }

        return config.getCommandText();
    }

    /**
     * Resolves a whitelisted streaming command (e.g. {@code TOMCAT_TAIL}) for WebSocket use.
     *
     * @param commandKey streaming command key
     * @return approved command text
     */
    @Transactional(readOnly = true)
    public String resolveStreamCommand(String commandKey) {
        LogConfig config = logConfigRepository.findByCommandKey(commandKey)
                .orElseThrow(() -> new CommandNotWhitelistedException(commandKey));

        if (!commandKey.endsWith(TAIL_KEY_SUFFIX)) {
            throw new CommandNotWhitelistedException(commandKey);
        }

        return config.getCommandText();
    }

    /**
     * Opens an SSH connection to an active server.
     *
     * @param serverId target server ID
     * @return active SSH connection (caller must disconnect)
     */
    public SshConnection openConnection(Long serverId) {
        Server server = getActiveServer(serverId);
        String privateKey = serverService.decryptPrivateKey(server);
        SshConnectionParams params = new SshConnectionParams(
                server.getHost(),
                server.getPort(),
                server.getUsername(),
                privateKey
        );
        return sshService.connect(params);
    }

    private String resolveSearchCommand(String commandKey, String sanitizedTerm) {
        String searchKey = toSearchCommandKey(commandKey);
        LogConfig config = logConfigRepository.findByCommandKey(searchKey)
                .orElseThrow(() -> new CommandNotWhitelistedException(searchKey));

        String template = config.getCommandText();
        if (!template.contains(SEARCH_PLACEHOLDER)) {
            throw new IllegalStateException(
                    "Search command template missing placeholder for key: " + searchKey);
        }

        return template.replace(SEARCH_PLACEHOLDER, escapeForSingleQuotedShell(sanitizedTerm));
    }

    private String toSearchCommandKey(String commandKey) {
        if (commandKey.endsWith(SEARCH_KEY_SUFFIX)) {
            return commandKey;
        }
        String baseKey = commandKey.endsWith(TAIL_KEY_SUFFIX)
                ? commandKey.substring(0, commandKey.length() - TAIL_KEY_SUFFIX.length())
                : commandKey;
        return baseKey + SEARCH_KEY_SUFFIX;
    }

    private String executeRemoteCommand(Server server, String command) {
        SshConnection connection = null;
        try {
            connection = openConnection(server.getId());
            return sshService.executeCommand(connection, command);
        } catch (SshOperationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new SshOperationException(
                    "Failed to execute log command on server: " + server.getServerName(), ex);
        } finally {
            sshService.disconnect(connection);
        }
    }

    private Server getActiveServer(Long serverId) {
        Server server = serverService.getServerEntity(serverId);
        if (!server.isActive()) {
            throw new ResourceNotFoundException("Server is inactive with id: " + serverId);
        }
        return server;
    }

    private boolean isFetchableLogType(LogConfig config) {
        String key = config.getCommandKey();
        return EXCLUDED_LOG_TYPE_SUFFIXES.stream().noneMatch(key::endsWith);
    }

    private String sanitizeSearchTerm(String searchTerm) {
        if (searchTerm == null || !SAFE_SEARCH_TERM.matcher(searchTerm).matches()) {
            throw new InvalidSearchTermException(
                    "Search term contains invalid characters. Allowed: letters, digits, "
                            + "spaces, dash, dot, underscore (max 100 characters)");
        }
        return searchTerm.trim();
    }

    private String escapeForSingleQuotedShell(String value) {
        return value.replace("'", "'\\''");
    }

    private List<String> splitLines(String output) {
        if (output == null || output.isBlank()) {
            return List.of();
        }
        return Arrays.asList(output.split("\\R"));
    }
}
