package com.logmonitor.exception;

/**
 * Thrown when a requested command key is not present in the log_config whitelist.
 */
public class CommandNotWhitelistedException extends RuntimeException {

    public CommandNotWhitelistedException(String commandKey) {
        super("Command key is not whitelisted: " + commandKey);
    }
}
