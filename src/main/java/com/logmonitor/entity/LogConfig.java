package com.logmonitor.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Objects;

/**
 * JPA entity mapping to the {@code log_config} table.
 *
 * <p>Defines whitelisted SSH commands that may be executed for log retrieval.</p>
 */
@Entity
@Table(name = "log_config")
public class LogConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "log_name", nullable = false, length = 100)
    private String logName;

    @Column(name = "command_key", nullable = false, unique = true, length = 50)
    private String commandKey;

    @Column(name = "command_text", nullable = false, columnDefinition = "TEXT")
    private String commandText;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLogName() {
        return logName;
    }

    public void setLogName(String logName) {
        this.logName = logName;
    }

    public String getCommandKey() {
        return commandKey;
    }

    public void setCommandKey(String commandKey) {
        this.commandKey = commandKey;
    }

    public String getCommandText() {
        return commandText;
    }

    public void setCommandText(String commandText) {
        this.commandText = commandText;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof LogConfig logConfig)) {
            return false;
        }
        return id != null && id.equals(logConfig.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "LogConfig{id=" + id + ", logName='" + logName + "', commandKey='"
                + commandKey + "'}";
    }
}
