package com.logmonitor.ssh;

import com.logmonitor.config.SshProperties;
import com.logmonitor.exception.SshConfigurationException;
import com.logmonitor.exception.SshOperationException;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.connection.channel.direct.Session.Command;
import net.schmizz.sshj.transport.verification.OpenSSHKnownHosts;
import net.schmizz.sshj.userauth.keyprovider.KeyProvider;
import net.schmizz.sshj.userauth.keyprovider.OpenSSHKeyFile;
import net.schmizz.sshj.userauth.keyprovider.PKCS8KeyFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * SSH gateway using SSHJ for remote command execution and log streaming.
 *
 * <p>Host keys are validated against a configured {@code known_hosts} file.
 * {@code PromiscuousVerifier} is never used.</p>
 */
@Service
public class SSHService {

    private static final Logger log = LoggerFactory.getLogger(SSHService.class);

    private final SshProperties sshProperties;

    public SSHService(SshProperties sshProperties) {
        this.sshProperties = sshProperties;
    }

    /**
     * Establishes an SSH connection with timeout and retry logic.
     *
     * @param params connection parameters including decrypted private key
     * @return active SSH connection
     */
    public SshConnection connect(SshConnectionParams params) {
        int maxRetries = sshProperties.getMaxRetries();
        long retryDelayMs = sshProperties.getRetryDelayMs();
        IOException lastException = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                SshConnection connection = doConnect(params);
                log.info("SSH connected to {}:{} on attempt {}", params.host(), params.port(), attempt);
                return connection;
            } catch (IOException ex) {
                lastException = ex;
                log.warn("SSH connection attempt {}/{} to {}:{} failed: {}",
                        attempt, maxRetries, params.host(), params.port(), ex.getMessage());
                if (attempt < maxRetries) {
                    sleepQuietly(retryDelayMs);
                }
            }
        }

        throw new SshOperationException(
                "Failed to connect to " + params.host() + ":" + params.port() + " after "
                        + maxRetries + " attempts",
                lastException
        );
    }

    /**
     * Executes a whitelisted command and returns stdout as a string.
     *
     * @param connection active SSH connection
     * @param command    pre-approved command text from log_config
     * @return command stdout
     */
    public String executeCommand(SshConnection connection, String command) {
        ensureConnected(connection);
        log.debug("Executing whitelisted command on {}:{}", connection.getHost(), connection.getPort());

        try {
            Session session = connection.getClient().startSession();
            try (session) {
                Command cmd = session.exec(command);
                String output = readStream(cmd);
                cmd.join(sshProperties.getCommandTimeoutMs(), TimeUnit.MILLISECONDS);

                Integer exitStatus = cmd.getExitStatus();
                if (exitStatus != null && exitStatus != 0) {
                    throw new SshOperationException(
                            "Command exited with status " + exitStatus + " on "
                                    + connection.getHost() + ":" + connection.getPort()
                    );
                }
                return output;
            }
        } catch (IOException ex) {
            throw new SshOperationException(
                    "Command execution failed on " + connection.getHost() + ":" + connection.getPort(),
                    ex
            );
        }
    }

    /**
     * Streams command output line-by-line (e.g. {@code tail -f}).
     *
     * <p>The consumer is invoked for each line until the stream ends or the thread is interrupted.
     * The caller is responsible for closing the connection when streaming completes.</p>
     *
     * @param connection   active SSH connection
     * @param command      pre-approved streaming command
     * @param lineConsumer callback invoked per output line
     */
    public void streamLogs(SshConnection connection, String command, Consumer<String> lineConsumer) {
        ensureConnected(connection);
        log.debug("Starting log stream on {}:{}", connection.getHost(), connection.getPort());

        try {
            Session session = connection.getClient().startSession();
            Command cmd = session.exec(command);

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(cmd.getInputStream(), StandardCharsets.UTF_8))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    if (Thread.currentThread().isInterrupted()) {
                        log.debug("Log stream interrupted for {}:{}", connection.getHost(), connection.getPort());
                        break;
                    }
                    lineConsumer.accept(line);
                }
            } finally {
                try {
                    cmd.close();
                } catch (IOException ex) {
                    log.debug("Error closing stream command: {}", ex.getMessage());
                }
                try {
                    session.close();
                } catch (IOException ex) {
                    log.debug("Error closing stream session: {}", ex.getMessage());
                }
            }
        } catch (IOException ex) {
            throw new SshOperationException(
                    "Log streaming failed on " + connection.getHost() + ":" + connection.getPort(),
                    ex
            );
        }
    }

    /**
     * Disconnects and releases an SSH connection.
     *
     * @param connection connection to close, may be {@code null}
     */
    public void disconnect(SshConnection connection) {
        if (connection != null) {
            connection.disconnect();
        }
    }

    private SshConnection doConnect(SshConnectionParams params) throws IOException {
        SSHClient client = new SSHClient();
        try {
            configureHostKeyVerification(client);
            client.setConnectTimeout((int) sshProperties.getConnectionTimeoutMs());
            client.setTimeout((int) sshProperties.getCommandTimeoutMs());

            client.connect(params.host(), params.port());
            client.authPublickey(params.username(), loadPrivateKey(params.privateKey()));

            return new SshConnection(client, params.host(), params.port());
        } catch (IOException | RuntimeException ex) {
            try {
                client.close();
            } catch (IOException closeEx) {
                log.debug("Error closing failed SSH client: {}", closeEx.getMessage());
            }
            if (ex instanceof IOException ioEx) {
                throw ioEx;
            }
            throw new IOException(ex.getMessage(), ex);
        }
    }

    private void configureHostKeyVerification(SSHClient client) throws IOException {
        String knownHostsPath = sshProperties.getKnownHostsPath();
        if (!StringUtils.hasText(knownHostsPath)) {
            throw new SshConfigurationException(
                    "SSH known_hosts path is not configured (log-monitor.ssh.known-hosts-path)");
        }

        File knownHostsFile = new File(knownHostsPath);
        if (!knownHostsFile.isFile()) {
            throw new SshConfigurationException("SSH known_hosts file not found: " + knownHostsPath);
        }

        client.addHostKeyVerifier(new OpenSSHKnownHosts(knownHostsFile));
        log.debug("Loaded SSH known_hosts from {}", knownHostsFile.getAbsolutePath());
    }

    private KeyProvider loadPrivateKey(String privateKeyPem) throws IOException {
        try {
            OpenSSHKeyFile openSshKey = new OpenSSHKeyFile();
            openSshKey.init(new StringReader(privateKeyPem));
            return openSshKey;
        } catch (IOException openSshFailure) {
            PKCS8KeyFile pkcs8Key = new PKCS8KeyFile();
            pkcs8Key.init(new StringReader(privateKeyPem));
            return pkcs8Key;
        }
    }

    private String readStream(Command cmd) throws IOException {
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(cmd.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!output.isEmpty()) {
                    output.append(System.lineSeparator());
                }
                output.append(line);
            }
        }
        return output.toString();
    }

    private void ensureConnected(SshConnection connection) {
        if (connection == null || !connection.isConnected()) {
            throw new SshOperationException("SSH connection is not active");
        }
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
