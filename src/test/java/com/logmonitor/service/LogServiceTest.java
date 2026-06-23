package com.logmonitor.service;

import com.logmonitor.audit.AuditService;
import com.logmonitor.dto.LogFetchRequest;
import com.logmonitor.dto.LogSearchRequest;
import com.logmonitor.entity.LogConfig;
import com.logmonitor.entity.Server;
import com.logmonitor.exception.CommandNotWhitelistedException;
import com.logmonitor.exception.InvalidSearchTermException;
import com.logmonitor.repository.LogConfigRepository;
import com.logmonitor.ssh.SSHService;
import com.logmonitor.ssh.SshConnection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link LogService} command whitelisting and search sanitization.
 */
@ExtendWith(MockitoExtension.class)
class LogServiceTest {

    @Mock
    private LogConfigRepository logConfigRepository;

    @Mock
    private ServerService serverService;

    @Mock
    private SSHService sshService;

    @Mock
    private AuditService auditService;

    @Mock
    private SshConnection sshConnection;

    private LogService logService;

    @BeforeEach
    void setUp() {
        logService = new LogService(logConfigRepository, serverService, sshService, auditService);
    }

    @Test
    void fetchLogs_executesWhitelistedCommandOnly() {
        Server server = activeServer();
        LogConfig config = fetchConfig("TOMCAT_LOG", "tail -n 1000 /var/log/tomcat/catalina.out");

        when(serverService.getServerEntity(1L)).thenReturn(server);
        when(logConfigRepository.findByCommandKey("TOMCAT_LOG")).thenReturn(Optional.of(config));
        when(serverService.decryptPrivateKey(server)).thenReturn("fake-key");
        when(sshService.connect(any())).thenReturn(sshConnection);
        when(sshService.executeCommand(sshConnection, config.getCommandText())).thenReturn("line1\nline2");

        LogFetchRequest request = new LogFetchRequest();
        request.setServerId(1L);
        request.setCommandKey("TOMCAT_LOG");

        var response = logService.fetchLogs(request);

        assertThat(response.getLines()).containsExactly("line1", "line2");
        verify(sshService).executeCommand(sshConnection, "tail -n 1000 /var/log/tomcat/catalina.out");
        verify(auditService).auditLogFetch("SYSTEM", 1L, "TOMCAT_LOG");
    }

    @Test
    void fetchLogs_rejectsNonWhitelistedCommand() {
        when(serverService.getServerEntity(1L)).thenReturn(activeServer());
        when(logConfigRepository.findByCommandKey("rm -rf /")).thenReturn(Optional.empty());

        LogFetchRequest request = new LogFetchRequest();
        request.setServerId(1L);
        request.setCommandKey("rm -rf /");

        assertThatThrownBy(() -> logService.fetchLogs(request))
                .isInstanceOf(CommandNotWhitelistedException.class);
    }

    @Test
    void fetchLogs_rejectsStreamingCommandKey() {
        when(serverService.getServerEntity(1L)).thenReturn(activeServer());
        LogConfig tailConfig = fetchConfig("TOMCAT_TAIL", "tail -f /var/log/tomcat/catalina.out");
        when(logConfigRepository.findByCommandKey("TOMCAT_TAIL")).thenReturn(Optional.of(tailConfig));

        LogFetchRequest request = new LogFetchRequest();
        request.setServerId(1L);
        request.setCommandKey("TOMCAT_TAIL");

        assertThatThrownBy(() -> logService.fetchLogs(request))
                .isInstanceOf(CommandNotWhitelistedException.class);
    }

    @Test
    void searchLogs_replacesSanitizedTermInWhitelistedTemplate() {
        Server server = activeServer();
        LogConfig searchConfig = fetchConfig(
                "TOMCAT_LOG_SEARCH",
                "grep -Fi '__SEARCH_TERM__' /var/log/tomcat/catalina.out | tail -n 500"
        );

        when(serverService.getServerEntity(1L)).thenReturn(server);
        when(logConfigRepository.findByCommandKey("TOMCAT_LOG_SEARCH")).thenReturn(Optional.of(searchConfig));
        when(serverService.decryptPrivateKey(server)).thenReturn("fake-key");
        when(sshService.connect(any())).thenReturn(sshConnection);
        when(sshService.executeCommand(eq(sshConnection), any())).thenReturn("match");

        LogSearchRequest request = new LogSearchRequest();
        request.setServerId(1L);
        request.setCommandKey("TOMCAT_LOG");
        request.setSearchTerm("ERROR");

        logService.searchLogs(request);

        verify(sshService).executeCommand(
                sshConnection,
                "grep -Fi 'ERROR' /var/log/tomcat/catalina.out | tail -n 500"
        );
    }

    @Test
    void searchLogs_rejectsUnsafeSearchTerm() {
        LogSearchRequest request = new LogSearchRequest();
        request.setServerId(1L);
        request.setCommandKey("TOMCAT_LOG");
        request.setSearchTerm("'; rm -rf / #");

        assertThatThrownBy(() -> logService.searchLogs(request))
                .isInstanceOf(InvalidSearchTermException.class);
    }

    @Test
    void getLogTypes_excludesSearchAndTailCommands() {
        when(logConfigRepository.findAllByOrderByLogNameAsc()).thenReturn(List.of(
                fetchConfig("TOMCAT_LOG", "tail -n 1000 /var/log/tomcat/catalina.out"),
                fetchConfig("TOMCAT_LOG_SEARCH", "grep ..."),
                fetchConfig("TOMCAT_TAIL", "tail -f ...")
        ));

        var types = logService.getLogTypes();

        assertThat(types).extracting("commandKey").containsExactly("TOMCAT_LOG");
    }

    private static Server activeServer() {
        Server server = new Server();
        server.setId(1L);
        server.setServerName("test-server");
        server.setHost("10.0.0.1");
        server.setPort(22);
        server.setUsername("deploy");
        server.setEncryptedPrivateKey("encrypted");
        server.setEnvironment("test");
        server.setActive(true);
        return server;
    }

    private static LogConfig fetchConfig(String key, String command) {
        LogConfig config = new LogConfig();
        config.setCommandKey(key);
        config.setCommandText(command);
        config.setLogName(key);
        return config;
    }
}
