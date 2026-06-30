package com.logmonitor.dto;

public class TomcatApplicationResponse {

    private Long id;
    private Long tomcatInstanceId;
    private String appName;
    private boolean logConfigCached;
    private String discoveredAt;

    public TomcatApplicationResponse() {}

    public TomcatApplicationResponse(Long id, Long tomcatInstanceId, String appName,
                                     boolean logConfigCached, String discoveredAt) {
        this.id = id;
        this.tomcatInstanceId = tomcatInstanceId;
        this.appName = appName;
        this.logConfigCached = logConfigCached;
        this.discoveredAt = discoveredAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTomcatInstanceId() { return tomcatInstanceId; }
    public void setTomcatInstanceId(Long tomcatInstanceId) { this.tomcatInstanceId = tomcatInstanceId; }
    public String getAppName() { return appName; }
    public void setAppName(String appName) { this.appName = appName; }
    public boolean isLogConfigCached() { return logConfigCached; }
    public void setLogConfigCached(boolean logConfigCached) { this.logConfigCached = logConfigCached; }
    public String getDiscoveredAt() { return discoveredAt; }
    public void setDiscoveredAt(String discoveredAt) { this.discoveredAt = discoveredAt; }
}
