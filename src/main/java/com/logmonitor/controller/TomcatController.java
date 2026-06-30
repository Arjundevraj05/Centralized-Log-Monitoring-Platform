package com.logmonitor.controller;

import com.logmonitor.dto.ApplicationLogConfigResponse;
import com.logmonitor.dto.TomcatApplicationResponse;
import com.logmonitor.dto.TomcatInstanceResponse;
import com.logmonitor.service.TomcatDiscoveryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/servers/{serverId}/tomcat")
@Tag(name = "Tomcat Discovery", description = "Discover Tomcat instances and applications on SSH servers")
public class TomcatController {

    private final TomcatDiscoveryService tomcatDiscoveryService;

    public TomcatController(TomcatDiscoveryService tomcatDiscoveryService) {
        this.tomcatDiscoveryService = tomcatDiscoveryService;
    }

    @GetMapping("/instances")
    @Operation(summary = "List cached Tomcat instances for a server")
    public List<TomcatInstanceResponse> listInstances(@PathVariable Long serverId) {
        return tomcatDiscoveryService.listInstances(serverId);
    }

    @PostMapping("/instances/discover")
    @Operation(summary = "Discover Tomcat installations under ~/local/apache-tomcat-*")
    public List<TomcatInstanceResponse> discoverInstances(@PathVariable Long serverId) {
        return tomcatDiscoveryService.discoverInstances(serverId);
    }

    @GetMapping("/instances/{instanceId}/applications")
    @Operation(summary = "List applications deployed in a Tomcat instance")
    public List<TomcatApplicationResponse> listApplications(
            @PathVariable Long serverId,
            @PathVariable Long instanceId) {
        return tomcatDiscoveryService.listApplications(serverId, instanceId);
    }

    @PostMapping("/instances/{instanceId}/applications/discover")
    @Operation(summary = "Discover applications from webapps directory")
    public List<TomcatApplicationResponse> discoverApplications(
            @PathVariable Long serverId,
            @PathVariable Long instanceId) {
        return tomcatDiscoveryService.discoverApplications(serverId, instanceId);
    }

    @PostMapping("/instances/{instanceId}/applications/{applicationId}/log-config/cache")
    @Operation(summary = "Read logback.xml and cache log paths for an application")
    public ApplicationLogConfigResponse cacheLogConfig(
            @PathVariable Long serverId,
            @PathVariable Long instanceId,
            @PathVariable Long applicationId) {
        return tomcatDiscoveryService.cacheLogConfig(serverId, instanceId, applicationId);
    }

    @GetMapping("/instances/{instanceId}/applications/{applicationId}/log-config")
    @Operation(summary = "Get cached logback paths for an application")
    public ApplicationLogConfigResponse getLogConfig(
            @PathVariable Long serverId,
            @PathVariable Long instanceId,
            @PathVariable Long applicationId) {
        return tomcatDiscoveryService.getLogConfig(serverId, instanceId, applicationId);
    }
}
