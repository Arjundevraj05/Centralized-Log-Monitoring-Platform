package com.logmonitor.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "application_log_configs")
public class ApplicationLogConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tomcat_application_id", nullable = false, unique = true)
    private TomcatApplication tomcatApplication;

    @Column(name = "logback_xml", nullable = false, columnDefinition = "TEXT")
    private String logbackXml;

    @Column(name = "current_log_path", nullable = false, length = 1024)
    private String currentLogPath;

    @Column(name = "archived_path_pattern", length = 1024)
    private String archivedPathPattern;

    @Column(name = "refreshed_at", nullable = false)
    private Instant refreshedAt = Instant.now();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TomcatApplication getTomcatApplication() {
        return tomcatApplication;
    }

    public void setTomcatApplication(TomcatApplication tomcatApplication) {
        this.tomcatApplication = tomcatApplication;
    }

    public String getLogbackXml() {
        return logbackXml;
    }

    public void setLogbackXml(String logbackXml) {
        this.logbackXml = logbackXml;
    }

    public String getCurrentLogPath() {
        return currentLogPath;
    }

    public void setCurrentLogPath(String currentLogPath) {
        this.currentLogPath = currentLogPath;
    }

    public String getArchivedPathPattern() {
        return archivedPathPattern;
    }

    public void setArchivedPathPattern(String archivedPathPattern) {
        this.archivedPathPattern = archivedPathPattern;
    }

    public Instant getRefreshedAt() {
        return refreshedAt;
    }

    public void setRefreshedAt(Instant refreshedAt) {
        this.refreshedAt = refreshedAt;
    }
}
