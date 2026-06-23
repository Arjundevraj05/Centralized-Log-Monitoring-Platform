package com.logmonitor.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request payload for creating or updating a server.
 */
@Schema(description = "Server registration or update request")
public class ServerRequest {

    @NotBlank(message = "Server name is required")
    @Size(max = 100, message = "Server name must not exceed 100 characters")
    @Schema(description = "Display name", example = "prod-tomcat-01")
    private String serverName;

    @NotBlank(message = "Host is required")
    @Size(max = 255, message = "Host must not exceed 255 characters")
    @Schema(description = "Hostname or IP address", example = "10.0.1.50")
    private String host;

    @Min(value = 1, message = "Port must be between 1 and 65535")
    @Max(value = 65535, message = "Port must be between 1 and 65535")
    @Schema(description = "SSH port", example = "22")
    private int port = 22;

    @NotBlank(message = "Username is required")
    @Size(max = 100, message = "Username must not exceed 100 characters")
    @Schema(description = "SSH username", example = "deploy")
    private String username;

    @Schema(description = "PEM private key (required on create, optional on update)")
    private String privateKey;

    @NotBlank(message = "Environment is required")
    @Size(max = 50, message = "Environment must not exceed 50 characters")
    @Schema(description = "Environment label", example = "prod")
    private String environment;

    @NotNull(message = "Active flag is required")
    @Schema(description = "Whether the server is active", example = "true")
    private Boolean active = true;

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

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
