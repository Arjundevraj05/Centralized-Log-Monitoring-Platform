package com.logmonitor.service;

import com.logmonitor.audit.AuditAction;
import com.logmonitor.audit.Audited;
import com.logmonitor.dto.ServerRequest;
import com.logmonitor.dto.ServerResponse;
import com.logmonitor.entity.Server;
import com.logmonitor.exception.ResourceNotFoundException;
import com.logmonitor.mapper.ServerMapper;
import com.logmonitor.repository.ServerRepository;
import com.logmonitor.util.EncryptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Service for managing SSH server registrations.
 */
@Service
@Transactional
public class ServerService {

    private static final Logger log = LoggerFactory.getLogger(ServerService.class);

    private final ServerRepository serverRepository;
    private final ServerMapper serverMapper;
    private final EncryptionService encryptionService;

    public ServerService(ServerRepository serverRepository,
                         ServerMapper serverMapper,
                         EncryptionService encryptionService) {
        this.serverRepository = serverRepository;
        this.serverMapper = serverMapper;
        this.encryptionService = encryptionService;
    }

    /**
     * Returns all registered servers.
     *
     * @return list of server responses
     */
    @Transactional(readOnly = true)
    public List<ServerResponse> findAll() {
        return serverRepository.findAll().stream()
                .map(serverMapper::toResponse)
                .toList();
    }

    /**
     * Returns a server by identifier.
     *
     * @param id server ID
     * @return server response
     */
    @Transactional(readOnly = true)
    public ServerResponse findById(Long id) {
        return serverMapper.toResponse(getServerEntity(id));
    }

    /**
     * Returns the server entity for internal use (e.g. SSH connections).
     *
     * @param id server ID
     * @return server entity
     */
    @Transactional(readOnly = true)
    public Server getServerEntity(Long id) {
        return serverRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Server not found with id: " + id));
    }

    /**
     * Creates a new server with an encrypted private key.
     *
     * @param request server details including PEM private key
     * @return created server response
     */
    @Audited(action = AuditAction.SERVER_CREATE, resourceSpel = "#request.serverName")
    public ServerResponse create(ServerRequest request) {
        validatePrivateKeyPresent(request.getPrivateKey(), true);

        if (serverRepository.existsByServerName(request.getServerName())) {
            throw new IllegalArgumentException("Server name already exists: " + request.getServerName());
        }

        Server server = mapRequestToEntity(new Server(), request);
        server.setEncryptedPrivateKey(encryptionService.encrypt(request.getPrivateKey()));

        Server saved = serverRepository.save(server);
        log.info("Created server: id={}, name={}", saved.getId(), saved.getServerName());
        return serverMapper.toResponse(saved);
    }

    /**
     * Updates an existing server. Private key is only changed when provided.
     *
     * @param id      server ID
     * @param request updated server details
     * @return updated server response
     */
    @Audited(action = AuditAction.SERVER_UPDATE, resourceSpel = "#request.serverName")
    public ServerResponse update(Long id, ServerRequest request) {
        Server server = getServerEntity(id);

        if (!server.getServerName().equals(request.getServerName())
                && serverRepository.existsByServerName(request.getServerName())) {
            throw new IllegalArgumentException("Server name already exists: " + request.getServerName());
        }

        mapRequestToEntity(server, request);
        if (StringUtils.hasText(request.getPrivateKey())) {
            server.setEncryptedPrivateKey(encryptionService.encrypt(request.getPrivateKey()));
        }

        Server saved = serverRepository.save(server);
        log.info("Updated server: id={}, name={}", saved.getId(), saved.getServerName());
        return serverMapper.toResponse(saved);
    }

    /**
     * Deletes a server by identifier.
     *
     * @param id server ID
     */
    @Audited(action = AuditAction.SERVER_DELETE, resourceSpel = "#id")
    public void delete(Long id) {
        Server server = getServerEntity(id);
        String serverName = server.getServerName();
        serverRepository.delete(server);
        log.info("Deleted server: id={}, name={}", id, serverName);
    }

    /**
     * Decrypts the stored private key for SSH use. Never log the result.
     *
     * @param server server entity
     * @return decrypted PEM private key
     */
    @Transactional(readOnly = true)
    public String decryptPrivateKey(Server server) {
        return encryptionService.decrypt(server.getEncryptedPrivateKey());
    }

    private Server mapRequestToEntity(Server server, ServerRequest request) {
        server.setServerName(request.getServerName());
        server.setHost(request.getHost());
        server.setPort(request.getPort());
        server.setUsername(request.getUsername());
        server.setEnvironment(request.getEnvironment());
        server.setActive(Boolean.TRUE.equals(request.getActive()));
        return server;
    }

    private void validatePrivateKeyPresent(String privateKey, boolean required) {
        if (required && !StringUtils.hasText(privateKey)) {
            throw new IllegalArgumentException("Private key is required when creating a server");
        }
    }
}
