package com.logmonitor.dto;

public class TomcatInstanceResponse {

    private Long id;
    private Long serverId;
    private String instanceName;
    private String catalinaHome;
    private String discoveredAt;

    public TomcatInstanceResponse() {}

    public TomcatInstanceResponse(Long id, Long serverId, String instanceName, String catalinaHome, String discoveredAt) {
        this.id = id;
        this.serverId = serverId;
        this.instanceName = instanceName;
        this.catalinaHome = catalinaHome;
        this.discoveredAt = discoveredAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getServerId() { return serverId; }
    public void setServerId(Long serverId) { this.serverId = serverId; }
    public String getInstanceName() { return instanceName; }
    public void setInstanceName(String instanceName) { this.instanceName = instanceName; }
    public String getCatalinaHome() { return catalinaHome; }
    public void setCatalinaHome(String catalinaHome) { this.catalinaHome = catalinaHome; }
    public String getDiscoveredAt() { return discoveredAt; }
    public void setDiscoveredAt(String discoveredAt) { this.discoveredAt = discoveredAt; }
}
