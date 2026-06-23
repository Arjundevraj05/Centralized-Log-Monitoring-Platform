package com.logmonitor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

/**
 * AES encryption configuration for sensitive data at rest (SSH private keys).
 */
@Validated
@ConfigurationProperties(prefix = "log-monitor.encryption")
public class EncryptionProperties {

    @NotBlank
    private String secretKey;

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }
}
