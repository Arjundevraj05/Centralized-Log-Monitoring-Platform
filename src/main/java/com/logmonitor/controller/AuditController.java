package com.logmonitor.controller;

import com.logmonitor.audit.AuditAction;
import com.logmonitor.audit.AuditService;
import com.logmonitor.dto.AuditLogResponse;
import com.logmonitor.entity.AuditLog;
import com.logmonitor.mapper.AuditLogMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for querying the audit trail.
 */
@RestController
@RequestMapping("/api/audit")
@Tag(name = "Audit", description = "Security audit log queries (ADMIN only)")
@SecurityRequirement(name = "bearerAuth")
public class AuditController {

    private final AuditService auditService;
    private final AuditLogMapper auditLogMapper;

    public AuditController(AuditService auditService, AuditLogMapper auditLogMapper) {
        this.auditService = auditService;
        this.auditLogMapper = auditLogMapper;
    }

    /**
     * Returns paginated audit log entries with optional filters.
     *
     * @param username optional username filter
     * @param action   optional action filter
     * @param pageable pagination parameters
     * @return page of audit entries
     */
    @GetMapping
    @Operation(
            summary = "Query audit logs",
            description = "Returns paginated audit trail entries, optionally filtered by username or action"
    )
    @ApiResponse(responseCode = "200", description = "Audit logs retrieved")
    public ResponseEntity<Page<AuditLogResponse>> getAuditLogs(
            @Parameter(description = "Filter by username")
            @RequestParam(required = false) String username,
            @Parameter(description = "Filter by action type", example = "USER_LOGIN")
            @RequestParam(required = false) AuditAction action,
            @PageableDefault(size = 20, sort = "timestamp") Pageable pageable) {

        Page<AuditLog> page;
        if (username != null && !username.isBlank()) {
            page = auditService.findByUsername(username, pageable);
        } else if (action != null) {
            page = auditService.findByAction(action, pageable);
        } else {
            page = auditService.findAll(pageable);
        }

        return ResponseEntity.ok(page.map(auditLogMapper::toResponse));
    }
}
