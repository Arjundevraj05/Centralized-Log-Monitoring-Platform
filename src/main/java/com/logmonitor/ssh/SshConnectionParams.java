package com.logmonitor.ssh;

/**
 * Connection parameters for establishing an SSH session.
 *
 * @param host       remote host address
 * @param port       SSH port
 * @param username   SSH username
 * @param privateKey decrypted PEM private key (never logged)
 */
public record SshConnectionParams(String host, int port, String username, String privateKey) {

    public SshConnectionParams {
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("SSH host is required");
        }
        if (port <= 0 || port > 65535) {
            throw new IllegalArgumentException("SSH port must be between 1 and 65535");
        }
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("SSH username is required");
        }
        if (privateKey == null || privateKey.isBlank()) {
            throw new IllegalArgumentException("SSH private key is required");
        }
    }
}
