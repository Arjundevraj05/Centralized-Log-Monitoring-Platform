package com.logmonitor.repository;

import com.logmonitor.entity.Server;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for {@link Server} persistence operations.
 */
@Repository
public interface ServerRepository extends JpaRepository<Server, Long> {

    /**
     * Returns all active servers.
     *
     * @return list of active servers
     */
    List<Server> findByActiveTrue();

    /**
     * Returns servers filtered by environment label.
     *
     * @param environment the environment name (e.g. prod, uat)
     * @return matching servers
     */
    List<Server> findByEnvironment(String environment);

    /**
     * Returns active servers for a given environment.
     *
     * @param environment the environment name
     * @return active servers in that environment
     */
    List<Server> findByEnvironmentAndActiveTrue(String environment);

    /**
     * Checks whether a server name already exists.
     *
     * @param serverName the server name to check
     * @return {@code true} if the name is taken
     */
    boolean existsByServerName(String serverName);
}
