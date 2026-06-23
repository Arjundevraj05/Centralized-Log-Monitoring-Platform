package com.logmonitor.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Server details returned by the API (excludes private key).
 */
@Schema(description = "Registered SSH server")
public class ServerResponse {

    @Schema(description = "Server identifier", example = "1")
    private Long id;

    @Schema(description = "Display name", example = "prod-tomcat-01")
    private String serverName;

    @Schema(description = "Hostname or IP", example = "10.0.1.50")
    private String host;

    @Schema(description = "SSH port", example = "22")
    private int port;

    @Schema(description = "SSH username", example = "deploy")
    private String username;

    @Schema(description = "Environment label", example = "prod")
    private String environment;

    @Schema(description = "Whether the server is active", example = "true")
    private boolean active;

    @Schema(description = "Creation timestamp")
    private Instant createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
