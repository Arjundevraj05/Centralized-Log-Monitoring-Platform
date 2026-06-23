package com.logmonitor.controller;

import com.logmonitor.dto.LogFetchRequest;
import com.logmonitor.dto.LogResponse;
import com.logmonitor.dto.LogSearchRequest;
import com.logmonitor.dto.LogTypeResponse;
import com.logmonitor.service.LogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for log fetching and searching.
 */
@RestController
@RequestMapping("/api")
@Tag(name = "Logs", description = "Log fetch, search, and type discovery")
@SecurityRequirement(name = "bearerAuth")
public class LogController {

    private final LogService logService;

    public LogController(LogService logService) {
        this.logService = logService;
    }

    /**
     * Returns available log types from the command whitelist.
     *
     * @return fetchable log types
     */
    @GetMapping("/log-types")
    @Operation(summary = "List log types", description = "Returns whitelisted fetchable log command keys")
    @ApiResponse(responseCode = "200", description = "Log types retrieved")
    public ResponseEntity<List<LogTypeResponse>> getLogTypes() {
        return ResponseEntity.ok(logService.getLogTypes());
    }

    /**
     * Fetches logs from a remote server using a whitelisted command.
     *
     * @param request fetch parameters
     * @return log lines
     */
    @PostMapping("/logs/fetch")
    @Operation(
            summary = "Fetch logs",
            description = "Executes a whitelisted tail command on the target server via SSH"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Logs fetched successfully",
                    content = @Content(
                            schema = @Schema(implementation = LogResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "serverId": 1,
                                      "commandKey": "TOMCAT_LOG",
                                      "lines": ["2024-01-01 INFO Server started"],
                                      "lineCount": 1
                                    }
                                    """)
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Invalid or non-whitelisted command key"),
            @ApiResponse(responseCode = "502", description = "SSH connection or command failure")
    })
    public ResponseEntity<LogResponse> fetchLogs(@Valid @RequestBody LogFetchRequest request) {
        return ResponseEntity.ok(logService.fetchLogs(request));
    }

    /**
     * Searches logs on a remote server using a whitelisted grep command.
     *
     * @param request search parameters
     * @return matching log lines
     */
    @PostMapping("/logs/search")
    @Operation(
            summary = "Search logs",
            description = "Searches logs using a whitelisted grep template (ADMIN and DEV only)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Search completed"),
            @ApiResponse(responseCode = "400", description = "Invalid search term or command key"),
            @ApiResponse(responseCode = "403", description = "SUPPORT role cannot search logs")
    })
    public ResponseEntity<LogResponse> searchLogs(@Valid @RequestBody LogSearchRequest request) {
        return ResponseEntity.ok(logService.searchLogs(request));
    }
}
