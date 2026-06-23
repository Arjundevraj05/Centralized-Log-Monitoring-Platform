package com.logmonitor.repository;

import com.logmonitor.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Repository for {@link AuditLog} persistence operations.
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /**
     * Returns audit entries for a specific user.
     *
     * @param username the username to filter by
     * @param pageable pagination parameters
     * @return page of audit logs
     */
    Page<AuditLog> findByUsername(String username, Pageable pageable);

    /**
     * Returns audit entries for a specific action type.
     *
     * @param action the action name to filter by
     * @param pageable pagination parameters
     * @return page of audit logs
     */
    Page<AuditLog> findByAction(String action, Pageable pageable);

    /**
     * Returns audit entries within a time range, newest first.
     *
     * @param start inclusive start timestamp
     * @param end   inclusive end timestamp
     * @param pageable pagination parameters
     * @return page of audit logs
     */
    Page<AuditLog> findByTimestampBetweenOrderByTimestampDesc(
            Instant start, Instant end, Pageable pageable);

    /**
     * Returns all audit entries ordered by timestamp descending.
     *
     * @param pageable pagination parameters
     * @return page of audit logs
     */
    Page<AuditLog> findAllByOrderByTimestampDesc(Pageable pageable);

    /**
     * Returns recent audit entries for a user and action combination.
     *
     * @param username the username to filter by
     * @param action   the action to filter by
     * @return matching audit logs
     */
    List<AuditLog> findByUsernameAndActionOrderByTimestampDesc(String username, String action);
}
