package com.logmonitor.service;

import com.logmonitor.audit.AuditService;
import com.logmonitor.dto.AppLogFetchRequest;
import com.logmonitor.dto.LogResponse;
import com.logmonitor.entity.ApplicationLogConfig;
import com.logmonitor.exception.InvalidLogPathException;
import com.logmonitor.exception.SshOperationException;
import com.logmonitor.ssh.SSHService;
import com.logmonitor.ssh.SshConnection;
import com.logmonitor.util.LogbackXmlParser;
import com.logmonitor.util.PathValidator;
import com.logmonitor.util.SecurityUtils;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
public class AppLogService {

    private final TomcatDiscoveryService tomcatDiscoveryService;
    private final LogService logService;
    private final SSHService sshService;
    private final WhitelistedCommandResolver commandResolver;
    private final PathValidator pathValidator;
    private final LogbackXmlParser logbackXmlParser;
    private final AuditService auditService;

    public AppLogService(TomcatDiscoveryService tomcatDiscoveryService,
                         LogService logService,
                         SSHService sshService,
                         WhitelistedCommandResolver commandResolver,
                         PathValidator pathValidator,
                         LogbackXmlParser logbackXmlParser,
                         AuditService auditService) {
        this.tomcatDiscoveryService = tomcatDiscoveryService;
        this.logService = logService;
        this.sshService = sshService;
        this.commandResolver = commandResolver;
        this.pathValidator = pathValidator;
        this.logbackXmlParser = logbackXmlParser;
        this.auditService = auditService;
    }

    public LogResponse fetchAppLogs(AppLogFetchRequest request) {
        ApplicationLogConfig config = tomcatDiscoveryService.getLogConfigEntity(request.getLogConfigId());
        Long serverId = tomcatDiscoveryService.resolveServerIdForLogConfig(request.getLogConfigId());
        String username = SecurityUtils.getCurrentUsernameOrDefault("SYSTEM");

        String logPath = resolveLogPath(config, request.getMode(), request.getLogDate());
        pathValidator.validateAbsolutePath(logPath);

        String commandKey = "ARCHIVED".equals(request.getMode()) ? "APP_LOG_ARCHIVED" : "APP_LOG_CURRENT";
        String command = commandResolver.resolve(commandKey, Map.of("__LOG_PATH__", logPath));

        String output = executeRemote(serverId, command);
        auditService.log(username, com.logmonitor.audit.AuditAction.APP_LOG_FETCH,
                "logConfigId=" + request.getLogConfigId() + ", mode=" + request.getMode());

        List<String> lines = splitLines(output);
        return new LogResponse(serverId, commandKey, lines);
    }

    public String resolveLiveStreamCommand(Long logConfigId) {
        ApplicationLogConfig config = tomcatDiscoveryService.getLogConfigEntity(logConfigId);
        String logPath = config.getCurrentLogPath();
        pathValidator.validateAbsolutePath(logPath);
        return commandResolver.resolve("APP_LOG_LIVE", Map.of("__LOG_PATH__", logPath));
    }

    public Long resolveServerId(Long logConfigId) {
        return tomcatDiscoveryService.resolveServerIdForLogConfig(logConfigId);
    }

    private String resolveLogPath(ApplicationLogConfig config, String mode, String logDate) {
        if ("ARCHIVED".equals(mode)) {
            if (logDate == null || logDate.isBlank()) {
                throw new InvalidLogPathException("logDate is required for archived logs");
            }
            return logbackXmlParser.resolveArchivedPath(config.getArchivedPathPattern(), logDate);
        }
        return config.getCurrentLogPath();
    }

    private String executeRemote(Long serverId, String command) {
        SshConnection connection = null;
        try {
            connection = logService.openConnection(serverId);
            return sshService.executeCommand(connection, command);
        } catch (SshOperationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new SshOperationException("Failed to fetch application logs", ex);
        } finally {
            sshService.disconnect(connection);
        }
    }

    private List<String> splitLines(String output) {
        if (output == null || output.isBlank()) {
            return List.of();
        }
        return Arrays.asList(output.split("\\R"));
    }
}
