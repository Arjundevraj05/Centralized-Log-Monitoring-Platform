package com.logmonitor.ssh;

import net.schmizz.sshj.SSHClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;

/**
 * Wrapper around an active {@link SSHClient} session.
 */
public class SshConnection implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(SshConnection.class);

    private final SSHClient client;
    private final String host;
    private final int port;
    private volatile boolean connected;

    public SshConnection(SSHClient client, String host, int port) {
        this.client = client;
        this.host = host;
        this.port = port;
        this.connected = client.isConnected();
    }

    /**
     * Returns the underlying SSHJ client for command execution.
     *
     * @return connected SSH client
     */
    public SSHClient getClient() {
        return client;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public boolean isConnected() {
        return connected && client.isConnected();
    }

    /**
     * Closes the SSH connection and releases resources.
     */
    @Override
    public void close() {
        disconnect();
    }

    /**
     * Disconnects the SSH session if still connected.
     */
    public void disconnect() {
        if (!connected) {
            return;
        }
        try {
            if (client.isConnected()) {
                client.disconnect();
            }
            log.debug("Disconnected SSH session from {}:{}", host, port);
        } catch (IOException ex) {
            log.warn("Error disconnecting SSH session from {}:{}: {}", host, port, ex.getMessage());
        } finally {
            connected = false;
            try {
                client.close();
            } catch (IOException ex) {
                log.debug("Error closing SSH client: {}", ex.getMessage());
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SshConnection that)) {
            return false;
        }
        return port == that.port && Objects.equals(host, that.host) && Objects.equals(client, that.client);
    }

    @Override
    public int hashCode() {
        return Objects.hash(client, host, port);
    }
}
