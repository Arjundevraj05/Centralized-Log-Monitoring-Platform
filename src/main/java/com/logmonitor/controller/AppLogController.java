package com.logmonitor.controller;

import com.logmonitor.dto.AppLogFetchRequest;
import com.logmonitor.dto.LogResponse;
import com.logmonitor.service.AppLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/app-logs")
@Tag(name = "Application Logs", description = "Fetch application logs from cached logback paths")
public class AppLogController {

    private final AppLogService appLogService;

    public AppLogController(AppLogService appLogService) {
        this.appLogService = appLogService;
    }

    @PostMapping("/fetch")
    @Operation(summary = "Fetch current or archived application logs")
    public LogResponse fetchAppLogs(@Valid @RequestBody AppLogFetchRequest request) {
        return appLogService.fetchAppLogs(request);
    }
}
