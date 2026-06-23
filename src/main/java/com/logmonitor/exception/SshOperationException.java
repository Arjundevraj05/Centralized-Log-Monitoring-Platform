package com.logmonitor.exception;

/**
 * Thrown when SSH connection or command execution fails.
 */
public class SshOperationException extends RuntimeException {

    public SshOperationException(String message) {
        super(message);
    }

    public SshOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
