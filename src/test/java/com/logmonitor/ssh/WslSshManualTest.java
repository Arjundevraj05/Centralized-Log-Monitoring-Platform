package com.logmonitor.ssh;

import com.logmonitor.util.EncryptionService;
import com.logmonitor.config.EncryptionProperties;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.userauth.keyprovider.OpenSSHKeyFile;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Manual WSL SSH connectivity check — run when debugging local Tomcat setup.
 */
class WslSshManualTest {

    @Test
    void connectWithWslKeyFile() throws Exception {
        String keyPem = Files.readString(Path.of(System.getenv("USERPROFILE"), ".ssh", "wsl-logmonitor-test.key"));
        testConnect(keyPem);
    }

    @Test
    void connectAfterEncryptionRoundTrip() throws Exception {
        String keyPem = Files.readString(Path.of(System.getenv("USERPROFILE"), ".ssh", "wsl-logmonitor-test.key"));
        EncryptionProperties props = new EncryptionProperties();
        props.setSecretKey("local-dev-encryption-key-32b!");
        EncryptionService encryption = new EncryptionService(props);
        String encrypted = encryption.encrypt(keyPem);
        String decrypted = encryption.decrypt(encrypted);
        testConnect(decrypted);
    }

    private void testConnect(String keyPem) throws Exception {
        OpenSSHKeyFile key = new OpenSSHKeyFile();
        key.init(new StringReader(keyPem));

        try (SSHClient client = new SSHClient()) {
            client.addHostKeyVerifier(new PromiscuousVerifier());
            client.connect("172.22.235.173", 22);
            client.authPublickey("arjun", key);
            try (var session = client.startSession()) {
                var cmd = session.exec("tail -n 1 /var/log/tomcat/catalina.out");
                cmd.join(5000, java.util.concurrent.TimeUnit.MILLISECONDS);
                System.out.println(new String(cmd.getInputStream().readAllBytes()));
            }
        }
    }
}
