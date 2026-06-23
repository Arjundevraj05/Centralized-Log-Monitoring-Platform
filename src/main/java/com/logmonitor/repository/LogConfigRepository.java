package com.logmonitor.repository;

import com.logmonitor.entity.LogConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link LogConfig} whitelist entries.
 */
@Repository
public interface LogConfigRepository extends JpaRepository<LogConfig, Long> {

    /**
     * Finds a whitelisted log command by its lookup key.
     *
     * @param commandKey the command key (e.g. TOMCAT_LOG)
     * @return optional containing the configuration if found
     */
    Optional<LogConfig> findByCommandKey(String commandKey);

    /**
     * Checks whether a command key is registered in the whitelist.
     *
     * @param commandKey the command key to check
     * @return {@code true} if the key exists
     */
    boolean existsByCommandKey(String commandKey);

    /**
     * Returns all log configurations ordered by display name.
     *
     * @return all whitelist entries
     */
    List<LogConfig> findAllByOrderByLogNameAsc();
}
