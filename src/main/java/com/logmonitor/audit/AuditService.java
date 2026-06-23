package com.logmonitor.audit;

import com.logmonitor.entity.AuditLog;
import com.logmonitor.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Service responsible for persisting security audit trail entries.
 *
 * <p>Audit records are written in a separate transaction so they survive
 * rollbacks of the business operation being audited.</p>
 */
@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Persists a generic audit log entry.
     *
     * @param username the acting user
     * @param action   the action performed
     * @param resource optional resource identifier (server name, command key, etc.)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String username, AuditAction action, String resource) {
        AuditLog auditLog = new AuditLog();
        auditLog.setUsername(username);
        auditLog.setAction(action.name());
        auditLog.setResource(resource);
        auditLog.setTimestamp(Instant.now());

        auditLogRepository.save(auditLog);
        log.info("Audit recorded: user={}, action={}, resource={}", username, action.name(), resource);
    }

    /**
     * Records a successful user login event.
     *
     * @param username the authenticated username
     */
    public void auditLogin(String username) {
        log(username, AuditAction.USER_LOGIN, username);
    }

    /**
     * Records a log fetch operation.
     *
     * @param username   the acting user
     * @param serverId   the target server identifier
     * @param commandKey the whitelisted command key used
     */
    public void auditLogFetch(String username, Long serverId, String commandKey) {
        log(username, AuditAction.LOG_FETCH, formatLogResource(serverId, commandKey));
    }

    /**
     * Records a log search operation.
     *
     * @param username   the acting user
     * @param serverId   the target server identifier
     * @param commandKey the whitelisted command key used
     */
    public void auditLogSearch(String username, Long serverId, String commandKey) {
        log(username, AuditAction.LOG_SEARCH, formatLogResource(serverId, commandKey));
    }

    /**
     * Records server creation.
     *
     * @param username   the acting user
     * @param serverName the name of the created server
     */
    public void auditServerCreate(String username, String serverName) {
        log(username, AuditAction.SERVER_CREATE, serverName);
    }

    /**
     * Records server update.
     *
     * @param username   the acting user
     * @param serverName the name of the updated server
     */
    public void auditServerUpdate(String username, String serverName) {
        log(username, AuditAction.SERVER_UPDATE, serverName);
    }

    /**
     * Records server deletion.
     *
     * @param username   the acting user
     * @param serverName the name of the deleted server
     */
    public void auditServerDelete(String username, String serverName) {
        log(username, AuditAction.SERVER_DELETE, serverName);
    }

    /**
     * Returns paginated audit logs ordered by timestamp descending.
     *
     * @param pageable pagination parameters
     * @return page of audit entries
     */
    @Transactional(readOnly = true)
    public Page<AuditLog> findAll(Pageable pageable) {
        return auditLogRepository.findAllByOrderByTimestampDesc(pageable);
    }

    /**
     * Returns paginated audit logs filtered by username.
     *
     * @param username the username to filter by
     * @param pageable pagination parameters
     * @return page of audit entries
     */
    @Transactional(readOnly = true)
    public Page<AuditLog> findByUsername(String username, Pageable pageable) {
        return auditLogRepository.findByUsername(username, pageable);
    }

    /**
     * Returns paginated audit logs filtered by action type.
     *
     * @param action   the action name to filter by
     * @param pageable pagination parameters
     * @return page of audit entries
     */
    @Transactional(readOnly = true)
    public Page<AuditLog> findByAction(AuditAction action, Pageable pageable) {
        return auditLogRepository.findByAction(action.name(), pageable);
    }

    private static String formatLogResource(Long serverId, String commandKey) {
        return "serverId=" + serverId + ", commandKey=" + commandKey;
    }
}
