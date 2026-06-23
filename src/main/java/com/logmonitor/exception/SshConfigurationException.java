package com.logmonitor.exception;

/**
 * Thrown when SSH host key verification cannot be configured.
 */
public class SshConfigurationException extends RuntimeException {

    public SshConfigurationException(String message) {
        super(message);
    }
}
