package com.logmonitor.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "tomcat_applications")
public class TomcatApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tomcat_instance_id", nullable = false)
    private TomcatInstance tomcatInstance;

    @Column(name = "app_name", nullable = false)
    private String appName;

    @Column(name = "discovered_at", nullable = false)
    private Instant discoveredAt = Instant.now();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TomcatInstance getTomcatInstance() {
        return tomcatInstance;
    }

    public void setTomcatInstance(TomcatInstance tomcatInstance) {
        this.tomcatInstance = tomcatInstance;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public Instant getDiscoveredAt() {
        return discoveredAt;
    }

    public void setDiscoveredAt(Instant discoveredAt) {
        this.discoveredAt = discoveredAt;
    }
}
