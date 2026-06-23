package com.logmonitor.audit;

/**
 * Enumerated audit action types tracked by the platform.
 */
public enum AuditAction {

    USER_LOGIN("User login"),
    LOG_FETCH("Log fetch"),
    LOG_SEARCH("Log search"),
    SERVER_CREATE("Server creation"),
    SERVER_UPDATE("Server update"),
    SERVER_DELETE("Server deletion");

    private final String description;

    AuditAction(String description) {
        this.description = description;
    }

    /**
     * Returns a human-readable description of this action.
     *
     * @return action description
     */
    public String getDescription() {
        return description;
    }
}
