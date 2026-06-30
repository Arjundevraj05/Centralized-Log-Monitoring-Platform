package com.logmonitor.service;

import com.logmonitor.audit.AuditService;
import com.logmonitor.dto.ApplicationLogConfigResponse;
import com.logmonitor.dto.TomcatApplicationResponse;
import com.logmonitor.dto.TomcatInstanceResponse;
import com.logmonitor.entity.ApplicationLogConfig;
import com.logmonitor.entity.Server;
import com.logmonitor.entity.TomcatApplication;
import com.logmonitor.entity.TomcatInstance;
import com.logmonitor.exception.ResourceNotFoundException;
import com.logmonitor.exception.SshOperationException;
import com.logmonitor.repository.ApplicationLogConfigRepository;
import com.logmonitor.repository.TomcatApplicationRepository;
import com.logmonitor.repository.TomcatInstanceRepository;
import com.logmonitor.ssh.SSHService;
import com.logmonitor.ssh.SshConnection;
import com.logmonitor.util.LogbackXmlParser;
import com.logmonitor.util.PathValidator;
import com.logmonitor.util.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TomcatDiscoveryService {

    private static final Logger log = LoggerFactory.getLogger(TomcatDiscoveryService.class);
    private static final Set<String> SKIP_WEBAPPS = Set.of("ROOT", "docs", "examples", "host-manager", "manager");

    private final TomcatInstanceRepository tomcatInstanceRepository;
    private final TomcatApplicationRepository tomcatApplicationRepository;
    private final ApplicationLogConfigRepository applicationLogConfigRepository;
    private final ServerService serverService;
    private final LogService logService;
    private final SSHService sshService;
    private final WhitelistedCommandResolver commandResolver;
    private final PathValidator pathValidator;
    private final LogbackXmlParser logbackXmlParser;
    private final AuditService auditService;

    public TomcatDiscoveryService(TomcatInstanceRepository tomcatInstanceRepository,
                                  TomcatApplicationRepository tomcatApplicationRepository,
                                  ApplicationLogConfigRepository applicationLogConfigRepository,
                                  ServerService serverService,
                                  LogService logService,
                                  SSHService sshService,
                                  WhitelistedCommandResolver commandResolver,
                                  PathValidator pathValidator,
                                  LogbackXmlParser logbackXmlParser,
                                  AuditService auditService) {
        this.tomcatInstanceRepository = tomcatInstanceRepository;
        this.tomcatApplicationRepository = tomcatApplicationRepository;
        this.applicationLogConfigRepository = applicationLogConfigRepository;
        this.serverService = serverService;
        this.logService = logService;
        this.sshService = sshService;
        this.commandResolver = commandResolver;
        this.pathValidator = pathValidator;
        this.logbackXmlParser = logbackXmlParser;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<TomcatInstanceResponse> listInstances(Long serverId) {
        ensureActiveServer(serverId);
        return tomcatInstanceRepository.findByServerIdOrderByInstanceNameAsc(serverId).stream()
                .map(this::toInstanceResponse)
                .toList();
    }

    @Transactional
    public List<TomcatInstanceResponse> discoverInstances(Long serverId) {
        Server server = ensureActiveServer(serverId);
        String username = SecurityUtils.getCurrentUsernameOrDefault("SYSTEM");
        String home = "/home/" + server.getUsername();

        String command = commandResolver.resolve("TOMCAT_DISCOVER", Map.of("__HOME__", home));
        String output = executeRemote(server.getId(), command);

        List<String> paths = parseLines(output).stream()
                .filter(path -> path.contains("apache-tomcat"))
                .toList();

        for (String path : paths) {
            pathValidator.validateCatalinaHome(path);
            String name = path.substring(path.lastIndexOf('/') + 1);
            TomcatInstance instance = tomcatInstanceRepository
                    .findByServerIdAndCatalinaHome(serverId, path)
                    .orElseGet(() -> {
                        TomcatInstance created = new TomcatInstance();
                        created.setServer(server);
                        return created;
                    });
            instance.setInstanceName(name);
            instance.setCatalinaHome(path);
            instance.setDiscoveredAt(Instant.now());
            tomcatInstanceRepository.save(instance);
        }

        // Remove stale instances not found in latest discovery
        Set<String> discoveredPaths = Set.copyOf(paths);
        tomcatInstanceRepository.findByServerIdOrderByInstanceNameAsc(serverId).stream()
                .filter(i -> !discoveredPaths.contains(i.getCatalinaHome()))
                .forEach(tomcatInstanceRepository::delete);

        auditService.log(username, com.logmonitor.audit.AuditAction.TOMCAT_DISCOVER,
                "serverId=" + serverId + ", instances=" + paths.size());

        return listInstances(serverId);
    }

    @Transactional(readOnly = true)
    public List<TomcatApplicationResponse> listApplications(Long serverId, Long instanceId) {
        TomcatInstance instance = getInstance(serverId, instanceId);
        return tomcatApplicationRepository.findByTomcatInstanceIdOrderByAppNameAsc(instance.getId()).stream()
                .map(this::toApplicationResponse)
                .toList();
    }

    @Transactional
    public List<TomcatApplicationResponse> discoverApplications(Long serverId, Long instanceId) {
        TomcatInstance instance = getInstance(serverId, instanceId);
        pathValidator.validateCatalinaHome(instance.getCatalinaHome());

        String command = commandResolver.resolve("TOMCAT_LIST_WEBAPPS",
                Map.of("__CATALINA_HOME__", instance.getCatalinaHome()));
        String output = executeRemote(serverId, command);

        List<String> apps = parseLines(output).stream()
                .map(name -> name.replaceAll("\\.war$", ""))
                .filter(name -> !SKIP_WEBAPPS.contains(name))
                .filter(name -> !name.startsWith("."))
                .distinct()
                .toList();

        for (String appName : apps) {
            pathValidator.validateAppName(appName);
            TomcatApplication application = tomcatApplicationRepository
                    .findByTomcatInstanceIdAndAppName(instance.getId(), appName)
                    .orElseGet(() -> {
                        TomcatApplication created = new TomcatApplication();
                        created.setTomcatInstance(instance);
                        return created;
                    });
            application.setAppName(appName);
            application.setDiscoveredAt(Instant.now());
            tomcatApplicationRepository.save(application);
        }

        Set<String> discoveredApps = Set.copyOf(apps);
        tomcatApplicationRepository.findByTomcatInstanceIdOrderByAppNameAsc(instance.getId()).stream()
                .filter(a -> !discoveredApps.contains(a.getAppName()))
                .forEach(tomcatApplicationRepository::delete);

        return listApplications(serverId, instanceId);
    }

    @Transactional
    public ApplicationLogConfigResponse cacheLogConfig(Long serverId, Long instanceId, Long applicationId) {
        TomcatApplication application = getApplication(serverId, instanceId, applicationId);
        TomcatInstance instance = application.getTomcatInstance();
        String classesPath = pathValidator.buildLogbackClassesPath(
                instance.getCatalinaHome(), application.getAppName());

        String command = commandResolver.resolve("TOMCAT_READ_LOGBACK",
                Map.of("__LOGBACK_PATH__", classesPath));
        String logbackXml = executeRemote(serverId, command);

        if (logbackXml.isBlank()) {
            throw new SshOperationException(
                    "logback.xml not found at " + classesPath + "/logback.xml");
        }

        LogbackXmlParser.ParsedLogback parsed = logbackXmlParser.parse(logbackXml);
        pathValidator.validateAbsolutePath(parsed.currentLogPath());
        if (parsed.archivedPathPattern() != null) {
            pathValidator.validateArchivedPattern(parsed.archivedPathPattern());
        }

        ApplicationLogConfig config = applicationLogConfigRepository
                .findByTomcatApplicationId(application.getId())
                .orElseGet(ApplicationLogConfig::new);

        config.setTomcatApplication(application);
        config.setLogbackXml(logbackXml);
        config.setCurrentLogPath(parsed.currentLogPath());
        config.setArchivedPathPattern(parsed.archivedPathPattern());
        config.setRefreshedAt(Instant.now());
        applicationLogConfigRepository.save(config);

        String username = SecurityUtils.getCurrentUsernameOrDefault("SYSTEM");
        auditService.log(username, com.logmonitor.audit.AuditAction.APP_LOG_CONFIG_CACHE,
                "serverId=" + serverId + ", app=" + application.getAppName());

        return toLogConfigResponse(config);
    }

    @Transactional(readOnly = true)
    public ApplicationLogConfig getLogConfigEntity(Long logConfigId) {
        return applicationLogConfigRepository.findById(logConfigId)
                .orElseThrow(() -> new ResourceNotFoundException("Log config not found: " + logConfigId));
    }

    @Transactional(readOnly = true)
    public ApplicationLogConfigResponse getLogConfig(Long serverId, Long instanceId, Long applicationId) {
        TomcatApplication application = getApplication(serverId, instanceId, applicationId);
        ApplicationLogConfig config = applicationLogConfigRepository.findByTomcatApplicationId(application.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Log config not cached for application: " + application.getAppName()));
        return toLogConfigResponse(config);
    }

    public Long resolveServerIdForLogConfig(Long logConfigId) {
        ApplicationLogConfig config = getLogConfigEntity(logConfigId);
        return config.getTomcatApplication().getTomcatInstance().getServer().getId();
    }

    private Server ensureActiveServer(Long serverId) {
        Server server = serverService.getServerEntity(serverId);
        if (!server.isActive()) {
            throw new ResourceNotFoundException("Server is inactive with id: " + serverId);
        }
        return server;
    }

    private TomcatInstance getInstance(Long serverId, Long instanceId) {
        return tomcatInstanceRepository.findByIdAndServerId(instanceId, serverId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Tomcat instance not found: " + instanceId + " on server " + serverId));
    }

    private TomcatApplication getApplication(Long serverId, Long instanceId, Long applicationId) {
        getInstance(serverId, instanceId);
        return tomcatApplicationRepository.findByIdAndTomcatInstanceId(applicationId, instanceId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Application not found: " + applicationId));
    }

    private String executeRemote(Long serverId, String command) {
        SshConnection connection = null;
        try {
            connection = logService.openConnection(serverId);
            return sshService.executeCommand(connection, command);
        } finally {
            sshService.disconnect(connection);
        }
    }

    private List<String> parseLines(String output) {
        if (output == null || output.isBlank()) {
            return List.of();
        }
        return Arrays.stream(output.split("\\R"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    private TomcatInstanceResponse toInstanceResponse(TomcatInstance instance) {
        return new TomcatInstanceResponse(
                instance.getId(),
                instance.getServer().getId(),
                instance.getInstanceName(),
                instance.getCatalinaHome(),
                instance.getDiscoveredAt().toString()
        );
    }

    private TomcatApplicationResponse toApplicationResponse(TomcatApplication app) {
        boolean cached = applicationLogConfigRepository.findByTomcatApplicationId(app.getId()).isPresent();
        return new TomcatApplicationResponse(
                app.getId(),
                app.getTomcatInstance().getId(),
                app.getAppName(),
                cached,
                app.getDiscoveredAt().toString()
        );
    }

    private ApplicationLogConfigResponse toLogConfigResponse(ApplicationLogConfig config) {
        return new ApplicationLogConfigResponse(
                config.getId(),
                config.getTomcatApplication().getId(),
                config.getCurrentLogPath(),
                config.getArchivedPathPattern(),
                config.getRefreshedAt().toString()
        );
    }
}
