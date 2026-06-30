package com.logmonitor.dto;

public class ApplicationLogConfigResponse {

    private Long id;
    private Long applicationId;
    private String currentLogPath;
    private String archivedPathPattern;
    private String refreshedAt;

    public ApplicationLogConfigResponse() {}

    public ApplicationLogConfigResponse(Long id, Long applicationId, String currentLogPath,
                                        String archivedPathPattern, String refreshedAt) {
        this.id = id;
        this.applicationId = applicationId;
        this.currentLogPath = currentLogPath;
        this.archivedPathPattern = archivedPathPattern;
        this.refreshedAt = refreshedAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getApplicationId() { return applicationId; }
    public void setApplicationId(Long applicationId) { this.applicationId = applicationId; }
    public String getCurrentLogPath() { return currentLogPath; }
    public void setCurrentLogPath(String currentLogPath) { this.currentLogPath = currentLogPath; }
    public String getArchivedPathPattern() { return archivedPathPattern; }
    public void setArchivedPathPattern(String archivedPathPattern) { this.archivedPathPattern = archivedPathPattern; }
    public String getRefreshedAt() { return refreshedAt; }
    public void setRefreshedAt(String refreshedAt) { this.refreshedAt = refreshedAt; }
}
